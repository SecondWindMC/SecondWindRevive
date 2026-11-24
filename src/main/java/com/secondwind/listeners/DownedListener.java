package com.secondwind.listeners;

import com.secondwind.DownedManager;
import com.secondwind.SecondWindPlugin;
import com.secondwind.Settings;
import com.secondwind.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public final class DownedListener implements Listener {

    private final SecondWindPlugin plugin;
    private final DownedManager mgr;

    public DownedListener(SecondWindPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.downedManager();
    }

    // ---- entering the downed state ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Settings cfg = plugin.sw();

        if (mgr.isBypassed(p.getUniqueId())) return;
        if (mgr.isDowned(p.getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        if (mgr.isProtected(p.getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.hasPermission("secondwind.bypass")) return;
        if (cfg.disabledWorlds.contains(p.getWorld().getName())) return;
        if (cfg.exemptCauses.contains(e.getCause())) return;

        if (p.getHealth() - e.getFinalDamage() <= 0) {
            e.setCancelled(true);
            mgr.enterDowned(p, e.getCause().name());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        // True deaths (bleed-out / execute / admin) already removed from the map.
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (mgr.isDowned(p.getUniqueId()) && plugin.sw().killOnLogout) {
            mgr.killDowned(p);
        }
    }

    // ---- restrictions while downed ----

    @EventHandler(ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof Player p && mgr.isDowned(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && mgr.isDowned(p.getUniqueId())) {
            e.setCancelled(true);
        }
        // Interrupt a channeler who takes damage
        if (plugin.sw().channelInterruptOnDamage && e.getEntity() instanceof Player victim) {
            for (var st : mgr.downedMap().values()) {
                if (victim.getUniqueId().equals(st.channelUser)) {
                    Player target = org.bukkit.Bukkit.getPlayer(st.playerId);
                    mgr.interruptChannel(st, target, true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (mgr.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (mgr.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player p && mgr.isDowned(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (mgr.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (mgr.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent e) {
        if (mgr.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (mgr.isDowned(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent e) {
        if (e.getEntity() instanceof Player p && mgr.isDowned(p.getUniqueId()) && e.isGliding()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (!mgr.isDowned(e.getPlayer().getUniqueId())) return;
        if (e.getCause() == TeleportCause.PLUGIN || e.getCause() == TeleportCause.UNKNOWN) return;
        e.setCancelled(true);
        Msg.send(e.getPlayer(), "teleport-blocked");
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!mgr.isDowned(e.getPlayer().getUniqueId())) return;
        String base = e.getMessage().substring(1).split(" ")[0].toLowerCase();
        base = base.contains(":") ? base.substring(base.indexOf(':') + 1) : base;
        if (plugin.sw().blockedCommands.contains(base)) {
            e.setCancelled(true);
            Msg.send(e.getPlayer(), "command-blocked");
        }
    }
}
