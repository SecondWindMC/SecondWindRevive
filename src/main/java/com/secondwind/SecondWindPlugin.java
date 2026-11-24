package com.secondwind;

import com.secondwind.commands.SwCommand;
import com.secondwind.listeners.DownedListener;
import com.secondwind.tasks.DownedTickTask;
import com.secondwind.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SecondWindPlugin extends JavaPlugin {

    private DownedManager downedManager;
    private Settings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        reload();

        downedManager = new DownedManager(this);

        Bukkit.getPluginManager().registerEvents(new DownedListener(this), this);
        new DownedTickTask(this).runTaskTimer(this, 0L, 2L);

        SwCommand cmd = new SwCommand(this);
        getCommand("secondwind").setExecutor(cmd);
        getCommand("secondwind").setTabCompleter(cmd);

        getLogger().info("SecondWind enabled.");
    }

    @Override
    public void onDisable() {
        if (downedManager != null) {
            downedManager.shutdown();
        }
    }

    public void reload() {
        reloadConfig();
        Msg.reload(this);
        this.settings = new Settings(getConfig());
    }

    public DownedManager downedManager() {
        return downedManager;
    }

    public Settings sw() {
        return settings;
    }
}
