package com.secondwind.tasks;

import com.secondwind.DownedManager;
import com.secondwind.DownedState;
import com.secondwind.DownedState.ChannelType;
import com.secondwind.SecondWindPlugin;
import com.secondwind.Settings;
import com.secondwind.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.scheduler.BukkitRunnable;

public final class DownedTickTask extends BukkitRunnable {

    private static final double STEP = 0.1; // seconds per run (2 ticks)

    private final SecondWindPlugin plugin;
    private final DownedManager mgr;

    public DownedTickTask(SecondWindPlugin plugin) {
        this.plugin = plugin;
        this.mgr = plugin.downedManager();
    }

    @Override
    public void run() {
        for (DownedState st : mgr.downedMap().values().toArray(DownedState[]::new)) {
            Player target = Bukkit.getPlayer(st.playerId);
            if (target == null || !target.isOnline()) continue;
            tickDowned(st, target);
        }
    }

    private void tickDowned(DownedState st, Player target) {
        Settings cfg = plugin.sw();

        // Enforce crawl pose
        if (cfg.allowCrawl && target.getPose() != Pose.SWIMMING) {
            target.setPose(Pose.SWIMMING);
        }

        // Bleed-out countdown
        st.bleedOutRemaining -= STEP;
        if (st.bleedOutRemaining <= 0) {
            mgr.bleedOut(target);
            return;
        }
        st.bossBar.setProgress(Math.max(0.0, Math.min(1.0,
                st.bleedOutRemaining / cfg.bleedOutSeconds)));
        updateBossBarViewers(st, target);

        // Channel progress
        if (st.channelUser != null) {
            Player ch = Bukkit.getPlayer(st.channelUser);
            if (ch == null || !mgr.channelerValid(ch, target)) {
                mgr.interruptChannel(st, target, true);
            } else {
                st.channelProgress += STEP;
                double needed = st.channelType == ChannelType.REVIVE
                        ? cfg.reviveSeconds : cfg.executeSeconds;
                int pct = (int) Math.min(100, st.channelProgress / needed * 100);
                String key = st.channelType == ChannelType.REVIVE
                        ? "actionbar-reviving" : "actionbar-executing";
                mgr.actionbar(ch, key, "player", target.getName(), "progress", pct);
                mgr.actionbar(target, st.channelType == ChannelType.REVIVE
                        ? "actionbar-being-revived" : "actionbar-being-executed",
                        "player", ch.getName(), "progress", pct);
                if (st.channelProgress >= needed) {
                    if (st.channelType == ChannelType.REVIVE) {
                        mgr.revive(target, ch);
                    } else {
                        mgr.execute(target, ch);
                    }
                    return;
                }
            }
        }

        // Look for a channeler if none active (revive preferred, then execute)
        if (st.channelUser == null) {
            Player reviver = mgr.findChanneler(st, target, ChannelType.REVIVE);
            if (reviver != null) {
                st.channelUser = reviver.getUniqueId();
                st.channelType = ChannelType.REVIVE;
                st.channelProgress = 0;
                mgr.debug(reviver.getName() + " started reviving " + target.getName());
            } else {
                Player executor = mgr.findChanneler(st, target, ChannelType.EXECUTE);
                if (executor != null) {
                    st.channelUser = executor.getUniqueId();
                    st.channelType = ChannelType.EXECUTE;
                    st.channelProgress = 0;
                    mgr.debug(executor.getName() + " started executing " + target.getName());
                }
            }
        }
    }

    private void updateBossBarViewers(DownedState st, Player target) {
        double r2 = plugin.sw().bossbarVisibleRadius * plugin.sw().bossbarVisibleRadius;
        for (Player viewer : target.getWorld().getPlayers()) {
            boolean inRange = viewer.equals(target)
                    || viewer.getLocation().distanceSquared(target.getLocation()) <= r2;
            boolean has = st.bossBar.getPlayers().contains(viewer);
            if (inRange && !has) st.bossBar.addPlayer(viewer);
            if (!inRange && has) st.bossBar.removePlayer(viewer);
        }
    }
}
