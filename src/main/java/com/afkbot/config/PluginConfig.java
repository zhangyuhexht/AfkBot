package com.afkbot.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 插件配置（config.yml）
 */
public final class PluginConfig {

    private final JavaPlugin plugin;

    private int maxBots;
    private boolean showInTab;
    private boolean invulnerable;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        this.maxBots = Math.max(1, Math.min(100, c.getInt("max-bots", 10)));
        this.showInTab = c.getBoolean("show-in-tab", true);
        this.invulnerable = c.getBoolean("invulnerable", true);
    }

    public int getMaxBots() {
        return maxBots;
    }

    public boolean isShowInTab() {
        return showInTab;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }
}
