package com.afkbot.nms;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * NMS 操作层：创建 / 移除假人、区块加载、Tab 列表数据包。
 *
 * <p>关键设计：假人不会加入 PlayerList（不占用服务器人数上限），
 * 但以真实 ServerPlayer 实体的形式存在于世界中，因此会像真实玩家一样
 * 按服务器的视距设置加载地形。</p>
 */
public final class Nms {

    private Nms() {
    }

    private static MinecraftServer server() {
        return ((CraftServer) Bukkit.getServer()).getServer();
    }

    public static ServerLevel level(org.bukkit.World world) {
        return ((CraftWorld) world).getHandle();
    }

    /**
     * 构造假人实体（尚未加入世界）
     */
    public static ServerPlayer createPlayer(UUID uuid, String name, org.bukkit.World world) {
        MinecraftServer server = server();
        ServerLevel level = level(world);
        GameProfile profile = new GameProfile(uuid, name);

        ServerPlayer sp = new ServerPlayer(server, level, profile, ClientInformation.createDefault());

        // 假连接：假人没有客户端，所有出站数据包直接丢弃
        FakeConnection conn = new FakeConnection();
        sp.connection = new ServerGamePacketListenerImpl(
                server, conn, sp, CommonListenerCookie.createInitial(profile, false));

        return sp;
    }

    /**
     * 把假人放入世界
     *
     * <p>isRealPlayer=true 之后，{@code addNewPlayer} 会让服务端按真实玩家的方式
     * 注册区块加载（遵循服务器视距 / 模拟距离）。</p>
     */
    public static void spawnIntoWorld(ServerPlayer sp, Location loc) {
        ServerLevel level = level(loc.getWorld());

        sp.absSnapTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        // 防推动：关闭物理碰撞（玩家挤不动它），关闭重力防止穿地
        sp.noPhysics = true;
        sp.setNoGravity(true);

        // 让 Paper(Moonrise) 区块系统把假人当作真实玩家，才会真正加载地形
        sp.isRealPlayer = true;

        level.addNewPlayer(sp);
        PlatformHooks.get().updateMaps(level, sp);
    }

    /**
     * 在同一世界内移动假人，并刷新区块加载范围
     */
    public static void moveTo(ServerPlayer sp, Location to) {
        ServerLevel level = sp.level();
        sp.absSnapTo(to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
        sp.setDeltaMovement(0, 0, 0);
        // updateMaps 会按假人当前位置重新计算需要加载的区块
        PlatformHooks.get().updateMaps(level, sp);
        level.getChunkSource().move(sp);
    }

    /**
     * 移除假人：服务端会自动注销区块加载
     */
    public static void remove(ServerPlayer sp) {
        ServerLevel level = sp.level();
        level.removePlayerImmediately(sp, Entity.RemovalReason.DISCARDED);
    }

    /**
     * 重置速度并锁回锚点，保证假人绝不会被推动 / 炸飞
     */
    public static void keepStill(ServerPlayer sp, Location anchor) {
        if (sp.isRemoved()) {
            return;
        }
        sp.setDeltaMovement(0, 0, 0);
        double dx = sp.getX() - anchor.getX();
        double dy = sp.getY() - anchor.getY();
        double dz = sp.getZ() - anchor.getZ();
        if (dx * dx + dy * dy + dz * dz > 0.04) {
            sp.absSnapTo(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getYaw(), anchor.getPitch());
            sp.setDeltaMovement(0, 0, 0);
        }
    }

    // ==================== Tab 列表 ====================

    /**
     * 构建「假人加入 Tab 列表」数据包
     *
     * <p>提前构建（哪怕当前无人在线）可以尽早暴露服务端 API 变化的问题。</p>
     */
    public static Packet<?> tabAddPacket(ServerPlayer bot) {
        return new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE),
                List.of(bot));
    }

    /**
     * 构建「假人移出 Tab 列表」数据包
     */
    public static Packet<?> tabRemovePacket(UUID botId) {
        return new ClientboundPlayerInfoRemovePacket(List.of(botId));
    }

    /**
     * 向真实玩家发送数据包
     */
    public static void send(Packet<?> packet, Player to) {
        ((CraftPlayer) to).getHandle().connection.send(packet);
    }
}
