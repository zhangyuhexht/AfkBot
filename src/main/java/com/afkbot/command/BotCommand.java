package com.afkbot.command;

import com.afkbot.AfkBotPlugin;
import com.afkbot.bot.BotManager;
import com.afkbot.bot.FakeBot;
import com.afkbot.nms.Nms;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /bot 命令处理
 */
public final class BotCommand implements TabExecutor {

    private final AfkBotPlugin plugin;

    public BotCommand(AfkBotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("afkbot.admin")) {
            plugin.getMessages().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create", "add", "spawn" -> handleCreate(sender, args);
            case "remove", "del", "delete", "kill" -> handleRemove(sender, args);
            case "list", "ls" -> handleList(sender);
            case "tp" -> handleTp(sender, args, true);
            case "tphere", "bring" -> handleTp(sender, args, false);
            case "removeall", "killall" -> handleRemoveAll(sender);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String name = args[1];
        Location loc;
        if (args.length == 3 || args.length == 4) {
            // 坐标必须一次性给全三个数值
            sendUsage(sender);
            return;
        }
        if (args.length >= 5) {
            // 支持指定坐标：/bot create <名称> <x> <y> <z>
            try {
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);
                org.bukkit.World world = sender instanceof Player p
                        ? p.getWorld()
                        : plugin.getServer().getWorlds().get(0);
                loc = new Location(world, x, y, z);
            } catch (NumberFormatException e) {
                sendUsage(sender);
                return;
            }
        } else if (sender instanceof Player p) {
            loc = p.getLocation();
        } else {
            // 控制台执行时生成在主世界出生点
            loc = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }

        BotManager.CreateResult result = plugin.getBotManager().create(
                name, loc, plugin.getConfig_().isInvulnerable(),
                plugin.getConfig_().isShowInTab(), plugin.getConfig_().getMaxBots());

        Map<String, String> ph = Map.of(
                "name", name,
                "world", loc.getWorld() == null ? "?" : loc.getWorld().getName(),
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ()),
                "max", String.valueOf(plugin.getConfig_().getMaxBots()));

        switch (result) {
            case SUCCESS -> plugin.getMessages().send(sender, "create-success", ph);
            case INVALID_NAME -> plugin.getMessages().send(sender, "create-invalid-name", ph);
            case DUPLICATE -> plugin.getMessages().send(sender, "create-duplicate", ph);
            case LIMIT_REACHED -> plugin.getMessages().send(sender, "create-limit", ph);
            case WORLD_ERROR -> plugin.getMessages().send(sender, "create-invalid-name", ph);
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String name = args[1];
        if (plugin.getBotManager().remove(name)) {
            plugin.getMessages().send(sender, "remove-success", Map.of("name", name));
        } else {
            plugin.getMessages().send(sender, "remove-not-found", Map.of("name", name));
        }
    }

    private void handleRemoveAll(CommandSender sender) {
        int n = plugin.getBotManager().removeAll();
        plugin.getMessages().send(sender, "removeall-success", Map.of("count", String.valueOf(n)));
    }

    private void handleList(CommandSender sender) {
        BotManager manager = plugin.getBotManager();
        if (manager.size() == 0) {
            plugin.getMessages().send(sender, "list-empty");
            return;
        }
        plugin.getMessages().send(sender, "list-header", Map.of(
                "count", String.valueOf(manager.size()),
                "max", String.valueOf(plugin.getConfig_().getMaxBots())));

        for (FakeBot bot : manager.getAll()) {
            sender.spigot().sendMessage(buildListLine(bot));
        }
    }

    /**
     * 构造列表条目：名称 + 坐标 + 可点击的 [传送到此] [带过来] 按钮
     */
    private TextComponent buildListLine(FakeBot bot) {
        Location loc = bot.getLocation();
        String line = plugin.getMessages().raw("list-entry", Map.of(
                "name", bot.getName(),
                "world", loc.getWorld() == null ? "?" : loc.getWorld().getName(),
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ())));

        TextComponent root = new TextComponent(TextComponent.fromLegacyText(line));

        root.addExtra(" ");
        root.addExtra(button(
                ChatColor.AQUA + "[传送到此]",
                plugin.getMessages().raw("list-hover-tp", Map.of("name", bot.getName())),
                "/bot tp " + bot.getName()));
        root.addExtra(" ");
        root.addExtra(button(
                ChatColor.YELLOW + "[带过来]",
                plugin.getMessages().raw("list-hover-tphere", Map.of("name", bot.getName())),
                "/bot tphere " + bot.getName()));
        return root;
    }

    private TextComponent button(String text, String hover, String command) {
        TextComponent c = new TextComponent(text);
        c.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        c.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
        return c;
    }

    private void handleTp(CommandSender sender, String[] args, boolean toBot) {
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String name = args[1];
        FakeBot bot = plugin.getBotManager().get(name);
        if (bot == null) {
            plugin.getMessages().send(sender, "remove-not-found", Map.of("name", name));
            return;
        }
        if (toBot) {
            if (!(sender instanceof Player p)) {
                plugin.getMessages().send(sender, "players-only");
                return;
            }
            p.teleport(bot.getLocation());
            plugin.getMessages().send(sender, "tp-success", Map.of("name", name));
        } else {
            if (!(sender instanceof Player p)) {
                plugin.getMessages().send(sender, "players-only");
                return;
            }
            Location dest = p.getLocation();
            if (bot.getLocation().getWorld() != dest.getWorld()) {
                // 跨世界传送会破坏假人的维度状态，直接拒绝
                plugin.getMessages().send(sender, "tphere-other-world", Map.of("name", name));
                return;
            }
            Nms.moveTo(bot.getHandle(), dest);
            bot.setAnchor(dest);
            plugin.getMessages().send(sender, "tphere-success", Map.of("name", name));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadAll();
        plugin.getMessages().send(sender, "reload-success");
    }

    private void sendUsage(CommandSender sender) {
        for (String line : plugin.getMessages().rawList("usage")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    line.replace("%prefix%", plugin.getMessages().raw("prefix"))));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("afkbot.admin")) {
            return out;
        }
        if (args.length == 1) {
            for (String s : List.of("create", "remove", "list", "tp", "tphere", "removeall", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && List.of("remove", "tp", "tphere").contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (FakeBot bot : plugin.getBotManager().getAll()) {
                if (bot.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(bot.getName());
                }
            }
        }
        return out;
    }
}
