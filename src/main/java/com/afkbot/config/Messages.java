package com.afkbot.config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 中文消息管理（messages.yml）
 */
public final class Messages {

    private final JavaPlugin plugin;
    private YamlConfiguration yaml;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        // 合并新增的默认键，防止升级后缺失
        YamlConfiguration def = YamlConfiguration.loadConfiguration(
                new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8));
        yaml = YamlConfiguration.loadConfiguration(file);
        yaml.setDefaults(def);
        try {
            yaml.options().copyDefaults(true);
            yaml.save(file);
        } catch (Exception ignored) {
        }
    }

    /** 获取原始消息（已替换变量、已上色） */
    public String raw(String key, Map<String, String> placeholders) {
        String msg = yaml.getString(key, key);
        msg = msg.replace("%prefix%", yaml.getString("prefix", ""));
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                msg = msg.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return color(msg);
    }

    public String raw(String key) {
        return raw(key, Collections.emptyMap());
    }

    public void send(CommandSender to, String key) {
        send(to, key, Collections.emptyMap());
    }

    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        String text = raw(key, placeholders);
        for (String line : text.split("\n")) {
            to.sendMessage(line);
        }
    }

    public List<String> rawList(String key) {
        List<String> lines = yaml.getStringList(key);
        if (lines.isEmpty()) {
            String single = yaml.getString(key);
            return single == null ? List.of() : List.of(single);
        }
        return lines;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
