package com.secondwind;

import com.secondwind.DownedState.ChannelType;
import com.secondwind.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DownedManager {

    private final SecondWindPlugin plugin;
    private final Map<UUID, DownedState> downed = new HashMap<>();
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private final Set<UUID> bypass = new HashSet<>();
    private NamespacedKey slowKey;

    public DownedManager(SecondWindPlugin plugin) {
        this.plugin = plugin;
        this.slowKey = new NamespacedKey(plugin, "downed_slow");
    }

    public boolean isDowned(UUID id) {
        return downed.containsKey(id);
    }

    public DownedState get(UUID id) {
        return downed.get(id);
    }

    public boolean isBypassed(UUID id) {
        return bypass.contains(id);
    }

    public boolean isProtected(UUID id) {
        Long until = protectedUntil.get(id);
        return until != null && until > System.currentTimeMillis();
    }

    public Map<UUID, DownedState> downedMap() {
        return downed;
    }

    /** Put a player into the downed state. Caller must have cancelled the lethal damage. */
    public void enterDowned(Player p, String causeLabel) {
        Settings cfg = plugin.sw();
        if (isDowned(p.getUniqueId())) return;

        p.setHealth(Math.min(cfg.downedHealth, p.getMaxHealth()));
        DownedState state = new DownedState(p, cfg.bleedOutSeconds, Msg.raw("bossbar-downed"));
        downed.put(p.getUniqueId(), state);

        applySlow(p);
        p.setPose(Pose.SWIMMING);
        p.setSneaking(false);
        p.setGliding(false);

        Msg.send(p, "downed-self");
        if (cfg.broadcastOnDown) {
            Bukkit.getOnlinePlayers().forEach(other -> {
                if (!other.equals(p)) Msg.send(other, "downed-broadcast", "player", p.getName());
            });
        }
        debug(p.getName() + " is downed (cause: " + causeLabel + ")");
    }

    /** Revive a downed player. reviver may be null (admin/console). */
    public void revive(Player p, Entity reviver) {
        Settings cfg = plugin.sw();
        DownedState st = downed.remove(p.getUniqueId());
        if (st == null) return;
        st.dispose();

        removeSlow(p);
        p.setPose(Pose.STANDING);
        p.setHealth(Math.min(cfg.revivedHealth, p.getMaxHealth()));
        protectedUntil.put(p.getUniqueId(),
                System.currentTimeMillis() + (long) (cfg.postReviveInvulnerableSeconds * 1000));

        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        Msg.send(p, "revived-self");
        if (reviver instanceof Player r) Msg.send(r, "revived-other", "player", p.getName());
        debug(p.getName() + " was revived by " + (reviver == null ? "console" : reviver.getName()));
    }

    /** True death by bleed-out. */
    public void bleedOut(Player p) {
        internalKill(p, null);
        debug(p.getName() + " bled out");
    }

    /** True death by execution. */
    public void execute(Player p, Entity killer) {
        internalKill(p, killer);
        debug(p.getName() + " was executed by " + (killer == null ? "console" : killer.getName()));
    }

    private void internalKill(Player p, Entity killer) {
        DownedState st = downed.remove(p.getUniqueId());
        if (st != null) st.dispose();
        removeSlow(p);
        p.setPose(Pose.STANDING);

        bypass.add(p.getUniqueId());
        try {
            if (killer != null) {
                p.damage(Double.MAX_VALUE / 4, killer);
            } else {
                p.damage(Double.MAX_VALUE / 4);
            }
            if (!p.isDead()) p.setHealth(0);
        } finally {
            bypass.remove(p.getUniqueId());
        }
    }

    /** Immediately drop the downed state without reviving (logout, disable). Kills the player. */
    public void killDowned(Player p) {
        if (isDowned(p.getUniqueId())) internalKill(p, null);
    }

    public void shutdown() {
        for (UUID id : new HashSet<>(downed.keySet())) {
            Player p = Bukkit.getPlayer(id);
            DownedState st = downed.remove(id);
            if (st != null) st.dispose();
            if (p != null) {
                removeSlow(p);
                p.setPose(Pose.STANDING);
                p.setHealth(0);
            }
        }
    }

    // ---- channel eligibility ----

    public boolean canRevive(Player candidate, Player target) {
        if (candidate.equals(target)) return false;
        Settings cfg = plugin.sw();
        if (cfg.revivers == Settings.ReviverMode.TEAM && !sameTeam(candidate, target)) return false;
        return true;
    }

    public boolean canExecute(Player candidate, Player target) {
        if (candidate.equals(target)) return false;
        Settings cfg = plugin.sw();
        return switch (cfg.executors) {
            case NONE -> false;
            case ANYONE -> true;
            case ENEMY -> !sameTeam(candidate, target);
        };
    }

    private boolean sameTeam(Player a, Player b) {
        Team ta = Bukkit.getScoreboardManager().getMainScoreboard().getEntityTeam(a);
        Team tb = Bukkit.getScoreboardManager().getMainScoreboard().getEntityTeam(b);
        return ta != null && ta.equals(tb);
    }

    public boolean channelerValid(Player channeler, Player target) {
        Settings cfg = plugin.sw();
        return channeler.isOnline()
                && channeler.isValid()
                && (channeler.getGameMode() == GameMode.SURVIVAL
                        || channeler.getGameMode() == GameMode.ADVENTURE)
                && channeler.isSneaking()
                && channeler.getWorld().equals(target.getWorld())
                && channeler.getLocation().distanceSquared(target.getLocation()) <= cfg.interactRadius * cfg.interactRadius;
    }

    /** Find the best eligible channeler for this downed player (REVIVE preferred over EXECUTE). */
    public Player findChanneler(DownedState st, Player target, ChannelType type) {
        Settings cfg = plugin.sw();
        for (Player candidate : target.getWorld().getPlayers()) {
            if (candidate.equals(target) || isDowned(candidate.getUniqueId())) continue;
            if (!channelerValid(candidate, target)) continue;
            if (type == ChannelType.REVIVE && !canRevive(candidate, target)) continue;
            if (type == ChannelType.EXECUTE && !canExecute(candidate, target)) continue;
            return candidate;
        }
        return null;
    }

    public void interruptChannel(DownedState st, Player target, boolean notify) {
        Player ch = st.channelUser == null ? null : Bukkit.getPlayer(st.channelUser);
        if (notify && ch != null) Msg.send(ch, "channel-interrupted");
        st.resetChannel();
    }

    private void applySlow(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        double mult = plugin.sw().allowCrawl ? plugin.sw().movementMultiplier - 1.0 : -1.0;
        attr.addTransientModifier(new AttributeModifier(
                slowKey, mult, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    private void removeSlow(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(slowKey))
                .findFirst()
                .ifPresent(attr::removeModifier);
    }

    public void actionbar(Player p, String key, Object... kv) {
        p.sendActionBar((Component) Msg.component(key, kv));
    }

    public void debug(String msg) {
        if (plugin.sw().debug) plugin.getLogger().info("[debug] " + msg);
    }
}
