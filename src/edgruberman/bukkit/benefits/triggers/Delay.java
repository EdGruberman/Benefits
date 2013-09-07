package edgruberman.bukkit.benefits.triggers;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.benefits.Command;
import edgruberman.bukkit.benefits.Contribution;
import edgruberman.bukkit.benefits.Trigger;

/** Applied after a specified delay interval from when contribution was contributed */
public class Delay extends Trigger implements Runnable {

    private static final long DEFAULT_DELAY = -1; // no delay
    private static final long DEFAULT_TOLERANCE = 30; // seconds
    private static final long TICKS_PER_SECOND = 20;

    private final long delay;
    private final long tolerance;
    private final TreeSet<Contribution> pending = new TreeSet<Contribution>(Collections.reverseOrder(Contribution.NEWEST_CONTRIBUTION_FIRST));
    private int taskId = -1;

    public Delay(final Command command, final ConfigurationSection definition) {
        super(command, definition);
        this.delay = definition.getLong("delay", Delay.DEFAULT_DELAY) * 1000;
        this.tolerance = definition.getLong("tolerance", Delay.DEFAULT_TOLERANCE) * Delay.TICKS_PER_SECOND;
    }

    @Override
    public Collection<Contribution> getPending() {
        return Collections.unmodifiableSet(this.pending);
    }

    @Override
    public void clear() {
        Bukkit.getScheduler().cancelTask(this.taskId);
        this.pending.clear();
    }

    @Override
    public void add(final Contribution contribution) {
        this.pending.add(contribution);
        if (this.taskId != -1) Bukkit.getScheduler().cancelTask(this.taskId);
        this.run();
    }

    @Override
    public void run() {
        if (this.pending.size() == 0) return;

        final Contribution contribution = this.pending.first();
        if (contribution.contributed + this.delay <= System.currentTimeMillis()) {
            this.command.dispatch(contribution);
            if (this.pending.size() == 0) return;
        }

        this.taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this.command.getCoordinator().plugin, this, this.tolerance);
    }

    @Override
    public boolean remove(final Contribution contribution) {
        final boolean removed = this.pending.remove(contribution);
        return removed;
    }

    @Override
    public String toString() {
        return super.toString("delay: " + (this.delay / 1000) + "; tolerance: " + (this.tolerance / Delay.TICKS_PER_SECOND));
    }

}
