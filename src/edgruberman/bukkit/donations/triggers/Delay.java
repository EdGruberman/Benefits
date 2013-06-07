package edgruberman.bukkit.donations.triggers;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Trigger;

/** Applied after a specified delay interval from when donation was contributed */
public class Delay extends Trigger implements Runnable {

    private static final long TICKS_PER_SECOND = 20;

    private final long delay;
    private final long tolerance;
    private final TreeSet<Donation> pending = new TreeSet<Donation>(Collections.reverseOrder(Donation.NEWEST_CONTRIBUTION_FIRST));
    private int taskId = -1;

    public Delay(final Command command, final ConfigurationSection definition) {
        super(command, definition);
        this.delay = definition.getLong("delay", -1) * 1000;
        this.tolerance = definition.getInt("tolerance", 30) * Delay.TICKS_PER_SECOND;
    }

    @Override
    public Collection<Donation> getPending() {
        return Collections.unmodifiableSet(this.pending);
    }

    @Override
    public void clear() {
        Bukkit.getScheduler().cancelTask(this.taskId);
        this.pending.clear();
    }

    @Override
    public void add(final Donation donation) {
        this.pending.add(donation);
        if (this.taskId != -1) Bukkit.getScheduler().cancelTask(this.taskId);
        this.run();
    }

    @Override
    public void run() {
        if (this.pending.size() == 0) return;

        final Donation donation = this.pending.first();
        if (donation.contributed + this.delay <= System.currentTimeMillis()) {
            this.command.dispatch(donation);
            if (this.pending.size() == 0) return;
        }

        this.taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this.command.getCoordinator().plugin, this, this.tolerance);
    }

    @Override
    public void remove(final Donation donation) {
        this.pending.remove(donation);
    }

    @Override
    public String toString() {
        return super.toString("delay: " + (this.delay / 1000) + "; tolerance: " + (this.tolerance / Delay.TICKS_PER_SECOND));
    }

}
