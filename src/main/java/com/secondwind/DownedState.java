package com.secondwind;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class DownedState {

    public enum ChannelType { REVIVE, EXECUTE }

    public final UUID playerId;
    public double bleedOutRemaining;
    public final BossBar bossBar;
    public UUID channelUser;
    public ChannelType channelType;
    public double channelProgress;

    public DownedState(Player player, double bleedOutSeconds, String barTitle) {
        this.playerId = player.getUniqueId();
        this.bleedOutRemaining = bleedOutSeconds;
        this.bossBar = Bukkit.createBossBar(barTitle, BarColor.RED, BarStyle.SOLID);
        this.bossBar.setProgress(1.0);
        this.bossBar.addPlayer(player);
    }

    public void resetChannel() {
        this.channelUser = null;
        this.channelType = null;
        this.channelProgress = 0.0;
    }

    public void dispose() {
        bossBar.removeAll();
    }
}
