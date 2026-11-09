package com.afkbot.bot;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 单个假人的封装
 */
public final class FakeBot {

    private final String name;
    private final UUID uuid;
    private final ServerPlayer handle;
    private final Location anchor;

    public FakeBot(String name, UUID uuid, ServerPlayer handle, Location anchor) {
        this.name = name;
        this.uuid = uuid;
        this.handle = handle;
        this.anchor = anchor;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public ServerPlayer getHandle() {
        return handle;
    }

    /** 假人的 Bukkit 包装 */
    public Player getBukkit() {
        return (Player) handle.getBukkitEntity();
    }

    /** 锚点位置（假人创建时的位置，用于防漂移）。返回副本，避免被外部误改 */
    public Location getAnchor() {
        return anchor.clone();
    }

    public void setAnchor(Location anchor) {
        this.anchor.setWorld(anchor.getWorld());
        this.anchor.setX(anchor.getX());
        this.anchor.setY(anchor.getY());
        this.anchor.setZ(anchor.getZ());
        this.anchor.setYaw(anchor.getYaw());
        this.anchor.setPitch(anchor.getPitch());
    }

    public Location getLocation() {
        return getBukkit().getLocation();
    }
}
