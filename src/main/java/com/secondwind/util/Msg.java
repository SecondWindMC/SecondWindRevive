package com.secondwind.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class Msg {

    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();
    private static Map<String, String> messages = new HashMap<>();
    private static String prefix = "";

    private Msg() {}

    public static void reload(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        messages = new HashMap<>();
        for (String key : yml.getKeys(false)) {
            messages.put(key, yml.getString(key, ""));
        }
        prefix = messages.getOrDefault("prefix", "");
    }

    public static String raw(String key, Object... kv) {
        String text = messages.getOrDefault(key, key);
        text = text.replace("{prefix}", prefix);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            text = text.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return text;
    }

    public static Component component(String key, Object... kv) {
        return AMP.deserialize(raw(key, kv));
    }

    public static void send(Player p, String key, Object... kv) {
        String text = raw(key, kv);
        if (text.isEmpty()) return;
        p.sendMessage(AMP.deserialize(text));
    }
}
