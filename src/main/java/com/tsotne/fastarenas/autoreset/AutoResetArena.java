package com.tsotne.fastarenas.autoreset;

import com.tsotne.fastarenas.FastArenas;
import com.tsotne.fastarenas.utils.LoadSchem;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class AutoResetArena {
    private final String name;
    private final long delayTicks;
    private final long offsetTicks;
    private final String resetMessage;
    private BukkitTask task;
    private long nextResetTimeMillis;

    public AutoResetArena(String name, long delayTicks, long offsetTicks, String resetMessage) {
        this.name = name;
        this.delayTicks = delayTicks;
        this.offsetTicks = offsetTicks;
        this.resetMessage = resetMessage;
        this.nextResetTimeMillis = System.currentTimeMillis() + offsetTicks * 50L;
    }

    public void start(FastArenas plugin) {
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            plugin.getLogger().info("Auto-reset for arena " + this.name);
            this.nextResetTimeMillis = System.currentTimeMillis() + this.delayTicks * 50L;
            LoadSchem.loadSchematic(plugin, this.name, null, () -> {
                String msg = plugin.getConfigManager().getarenamessage(this.name);
                if (!msg.isEmpty()) {
                    Bukkit.broadcastMessage(msg);
                }
            });
        }, this.offsetTicks, this.delayTicks);
    }

    public void stop() {
        if (this.task != null && !this.task.isCancelled()) {
            this.task.cancel();
        }
    }

    public String getName() {
        return this.name;
    }

    public long getMillisUntilReset() {
        return Math.max(0L, this.nextResetTimeMillis - System.currentTimeMillis());
    }
}
