package com.afkbot.listener;

import com.afkbot.AfkBotPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 假人相关事件监听
 */
public final class BotListener implements Listener {

    private final AfkBotPlugin plugin;

    public BotListener(AfkBotPlugin plugin) {
        this.plugin = plugin;
    }

    /** 假人无敌时取消一切伤害（UUID 集合判断，O(1)） */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig_().isInvulnerable()) {
            return;
        }
        if (plugin.getBotManager().isBot(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** 玩家进服时补发已有假人的 Tab 列表条目 */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getBotManager().sendTabTo(event.getPlayer(), plugin.getConfig_().isShowInTab());
    }
}
