package com.secondwind.commands;

import com.secondwind.SecondWindPlugin;
import com.secondwind.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class SwCommand implements CommandExecutor, TabCompleter {

    private final SecondWindPlugin plugin;

    public SwCommand(SecondWindPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Msg.component("help"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!checkAdmin(sender)) return true;
                plugin.reload();
                sender.sendMessage(Msg.component("reload-ok"));
            }
            case "status" -> {
                if (!checkAdmin(sender)) return true;
                Player target = args.length > 1 ? Bukkit.getPlayer(args[1])
                        : (sender instanceof Player p ? p : null);
                if (target == null) {
                    sender.sendMessage(Msg.component("player-not-found"));
                    return true;
                }
                boolean downed = plugin.downedManager().isDowned(target.getUniqueId());
                var st = plugin.downedManager().get(target.getUniqueId());
                sender.sendMessage(Msg.component("status-result",
                        "player", target.getName(),
                        "state", downed ? "DOWNED (" + String.format("%.0f", st.bleedOutRemaining) + "s left)"
                                : "normal"));
            }
            case "down" -> {
                if (!checkAdmin(sender)) return true;
                Player target = requireTarget(sender, args);
                if (target == null) return true;
                if (plugin.downedManager().isDowned(target.getUniqueId())) {
                    sender.sendMessage(Msg.component("already-downed", "player", target.getName()));
                    return true;
                }
                plugin.downedManager().enterDowned(target, "ADMIN");
            }
            case "revive" -> {
                if (!checkAdmin(sender)) return true;
                Player target = requireTarget(sender, args);
                if (target == null) return true;
                if (!plugin.downedManager().isDowned(target.getUniqueId())) {
                    sender.sendMessage(Msg.component("not-downed", "player", target.getName()));
                    return true;
                }
                plugin.downedManager().revive(target, sender instanceof Player p ? p : null);
            }
            case "execute" -> {
                if (!checkAdmin(sender)) return true;
                Player target = requireTarget(sender, args);
                if (target == null) return true;
                if (!plugin.downedManager().isDowned(target.getUniqueId())) {
                    sender.sendMessage(Msg.component("not-downed", "player", target.getName()));
                    return true;
                }
                plugin.downedManager().execute(target, sender instanceof Player p ? p : null);
            }
            default -> sender.sendMessage(Msg.component("help"));
        }
        return true;
    }

    private boolean checkAdmin(CommandSender sender) {
        if (sender.hasPermission("secondwind.admin")) return true;
        sender.sendMessage(Msg.component("no-permission"));
        return false;
    }

    private Player requireTarget(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Msg.component("usage-target"));
            return null;
        }
        Player t = Bukkit.getPlayer(args[1]);
        if (t == null) sender.sendMessage(Msg.component("player-not-found"));
        return t;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("reload", "status", "down", "revive", "execute")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        }
        return out;
    }
}
