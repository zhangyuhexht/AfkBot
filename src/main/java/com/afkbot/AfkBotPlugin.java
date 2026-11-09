package com.afkbot;

import com.afkbot.bot.BotManager;
import com.afkbot.command.BotCommand;
import com.afkbot.config.Messages;
import com.afkbot.config.PluginConfig;
import com.afkbot.listener.BotListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 挂机假人插件主类
 */
public final class AfkBotPlugin extends JavaPlugin {

    private PluginConfig config;
    private Messages messages;
    private BotManager botManager;

    @Override
    public void onEnable() {
        this.config = new PluginConfig(this);
        this.messages = new Messages(this);
        this.botManager = new BotManager(this);

        BotCommand cmd = new BotCommand(this);
        PluginCommand pluginCommand = getCommand("bot");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(cmd);
            pluginCommand.setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(new BotListener(this), this);

        getLogger().info("挂机假人插件已启用，假人上限：" + config.getMaxBots());
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.shutdown();
        }
        getLogger().info("挂机假人插件已卸载，所有假人已移除。");
    }

    /** 重载配置与消息，并同步一次 Tab 显示状态 */
    public void reloadAll() {
        config.load();
        messages.load();
        if (botManager != null) {
            botManager.syncTab(config.isShowInTab());
        }
    }

    public PluginConfig getConfig_() {
        return config;
    }

    public Messages getMessages() {
        return messages;
    }

    public BotManager getBotManager() {
        return botManager;
    }
}
