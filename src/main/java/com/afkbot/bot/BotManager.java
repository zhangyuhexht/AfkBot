package com.afkbot.bot;

import com.afkbot.nms.Nms;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 假人管理器：创建、移除、查询
 */
public final class BotManager {

    /** 创建结果 */
    public enum CreateResult {
        SUCCESS, INVALID_NAME, DUPLICATE, LIMIT_REACHED, WORLD_ERROR
    }

    private final Plugin plugin;
    private final Map<String, FakeBot> bots = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<UUID> botIds = new HashSet<>();
    private final BukkitTask keepTask;

    public BotManager(Plugin plugin) {
        this.plugin = plugin;
        // 每 5 tick 锁定一次假人位置，确保绝不被推动
        this.keepTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (FakeBot bot : bots.values()) {
                try {
                    Nms.keepStill(bot.getHandle(), bot.getAnchor());
                } catch (Throwable ignored) {
                    // 世界卸载等极端情况，忽略
                }
            }
        }, 5L, 5L);
    }

    /**
     * 创建假人
     */
    public CreateResult create(String name, Location loc, boolean invulnerable,
                               boolean showInTab, int maxBots) {
        if (!isValidName(name)) {
            return CreateResult.INVALID_NAME;
        }
        if (loc.getWorld() == null) {
            return CreateResult.WORLD_ERROR;
        }
        if (bots.size() >= maxBots) {
            return CreateResult.LIMIT_REACHED;
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (bots.containsKey(key) || Bukkit.getPlayerExact(name) != null) {
            return CreateResult.DUPLICATE;
        }

        UUID uuid = UUID.randomUUID();
        ServerPlayer sp = Nms.createPlayer(uuid, name, loc.getWorld());
        try {
            Nms.spawnIntoWorld(sp, loc.clone());
            if (invulnerable) {
                sp.getBukkitEntity().setInvulnerable(true);
            }
        } catch (Throwable t) {
            // 创建失败时清理残留实体，避免“幽灵”假人
            try {
                Nms.remove(sp);
            } catch (Throwable ignored) {
            }
            plugin.getLogger().warning("创建假人 " + name + " 失败: " + t);
            return CreateResult.WORLD_ERROR;
        }

        FakeBot bot = new FakeBot(name, uuid, sp, loc.clone());
        bots.put(key, bot);
        botIds.add(uuid);

        if (showInTab) {
            broadcastTabAdd(bot);
        }

        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        plugin.getLogger().info("已创建假人 " + name + " @ " + loc.getWorld().getName()
                + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        return CreateResult.SUCCESS;
    }

    /**
     * 移除假人
     */
    public boolean remove(String name) {
        FakeBot bot = bots.remove(name.toLowerCase(Locale.ROOT));
        if (bot == null) {
            return false;
        }
        botIds.remove(bot.getUuid());

        try {
            Packet<?> tabRemove = Nms.tabRemovePacket(bot.getUuid());
            for (Player p : Bukkit.getOnlinePlayers()) {
                Nms.send(tabRemove, p);
            }
        } catch (Throwable ignored) {
        }
        try {
            Nms.remove(bot.getHandle());
        } catch (Throwable t) {
            plugin.getLogger().warning("移除假人 " + name + " 时出现异常: " + t);
        }

        plugin.getLogger().info("已移除假人 " + name);
        return true;
    }

    /**
     * 移除全部假人
     */
    public int removeAll() {
        int n = bots.size();
        for (String name : bots.keySet().toArray(new String[0])) {
            remove(name);
        }
        return n;
    }

    /**
     * 玩家加入服务器时补发已有假人的 Tab 条目
     */
    public void sendTabTo(Player player, boolean showInTab) {
        if (!showInTab || bots.isEmpty()) {
            return;
        }
        try {
            for (FakeBot bot : bots.values()) {
                Nms.send(Nms.tabAddPacket(bot.getHandle()), player);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 重载配置后同步一次 Tab 列表（区分开启 / 关闭）
     */
    public void syncTab(boolean showInTab) {
        for (FakeBot bot : bots.values()) {
            try {
                if (showInTab) {
                    broadcastTabAdd(bot);
                } else {
                    Packet<?> remove = Nms.tabRemovePacket(bot.getUuid());
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        Nms.send(remove, p);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void broadcastTabAdd(FakeBot bot) {
        try {
            Packet<?> packet = Nms.tabAddPacket(bot.getHandle());
            for (Player p : Bukkit.getOnlinePlayers()) {
                Nms.send(packet, p);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("发送假人 Tab 数据包失败: " + t);
        }
    }

    public FakeBot get(String name) {
        return bots.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<FakeBot> getAll() {
        return Collections.unmodifiableCollection(bots.values());
    }

    /** 该 UUID 是否是假人（供事件监听快速判断） */
    public boolean isBot(UUID uuid) {
        return botIds.contains(uuid);
    }

    public int size() {
        return bots.size();
    }

    public void shutdown() {
        keepTask.cancel();
        removeAll();
    }

    /** 名称规则：3-16 位字母、数字、下划线 */
    public static boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }
}
