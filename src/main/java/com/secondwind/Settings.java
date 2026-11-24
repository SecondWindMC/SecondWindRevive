package com.secondwind;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class Settings {

    public enum ReviverMode { ANYONE, TEAM }
    public enum ExecutorMode { ENEMY, ANYONE, NONE }

    public final double downedHealth;
    public final double revivedHealth;
    public final double bleedOutSeconds;
    public final double reviveSeconds;
    public final double executeSeconds;
    public final double interactRadius;
    public final double movementMultiplier;
    public final double postReviveInvulnerableSeconds;
    public final double bossbarVisibleRadius;
    public final ReviverMode revivers;
    public final ExecutorMode executors;
    public final boolean channelInterruptOnDamage;
    public final boolean allowCrawl;
    public final boolean killOnLogout;
    public final boolean broadcastOnDown;
    public final boolean debug;
    public final Set<EntityDamageEvent.DamageCause> exemptCauses;
    public final Set<String> disabledWorlds;
    public final Set<String> blockedCommands;

    public Settings(ConfigurationSection root) {
        ConfigurationSection s = root.getConfigurationSection("settings");
        this.downedHealth = s.getDouble("downed-health", 4.0);
        this.revivedHealth = s.getDouble("revived-health", 6.0);
        this.bleedOutSeconds = s.getDouble("bleed-out-seconds", 60.0);
        this.reviveSeconds = s.getDouble("revive-seconds", 8.0);
        this.executeSeconds = s.getDouble("execute-seconds", 10.0);
        this.interactRadius = s.getDouble("interact-radius", 3.0);
        this.movementMultiplier = s.getDouble("movement-multiplier", 0.15);
        this.postReviveInvulnerableSeconds = s.getDouble("post-revive-invulnerable-seconds", 5.0);
        this.bossbarVisibleRadius = s.getDouble("bossbar-visible-radius", 16.0);
        this.revivers = ReviverMode.valueOf(s.getString("revivers", "ANYONE").toUpperCase());
        this.executors = ExecutorMode.valueOf(s.getString("executors", "ENEMY").toUpperCase());
        this.channelInterruptOnDamage = s.getBoolean("channel-interrupt-on-damage", true);
        this.allowCrawl = s.getBoolean("allow-crawl", true);
        this.killOnLogout = s.getBoolean("kill-on-logout", true);
        this.broadcastOnDown = s.getBoolean("broadcast-on-down", true);
        this.debug = s.getBoolean("debug", false);
        this.exemptCauses = EnumSet.noneOf(EntityDamageEvent.DamageCause.class);
        for (String c : s.getStringList("exempt-causes")) {
            try {
                exemptCauses.add(EntityDamageEvent.DamageCause.valueOf(c.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        this.disabledWorlds = Set.copyOf(s.getStringList("disabled-worlds"));
        this.blockedCommands = Set.copyOf(
                s.getStringList("blocked-commands").stream().map(String::toLowerCase).toList());
    }
}
