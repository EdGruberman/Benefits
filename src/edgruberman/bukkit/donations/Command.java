package edgruberman.bukkit.donations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;

import edgruberman.bukkit.donations.triggers.Trigger;

public final class Command {

    public final Benefit benefit;
    public final String name;
    public final Collection<String> dispatch = new ArrayList<String>();
    public final Collection<Trigger> triggers = new HashSet<Trigger>();
    public final Collection<Donation> pending = new HashSet<Donation>();

    Command(final Benefit benefit, final String name, final List<String> dispatch) {
        this.benefit = benefit;
        this.name = name;
        if (dispatch != null)
            for (final String d : dispatch)
                this.dispatch.add(d);
    }

    void clear() {
        for (final Trigger trigger : this.triggers) trigger.clear();
        this.triggers.clear();
        this.pending.clear();
    }

    public void add(final Donation donation) {
        this.pending.add(donation);
        for (final Trigger trigger : this.triggers) {
            this.getCoordinator().plugin.getLogger().finest("Adding " + donation.toString() + " to " + trigger.getPath());
            trigger.add(donation);
        }
    }

    public void dispatch(final Donation donation) {
        this.remove(donation);
        for (final String d : this.dispatch) {
            final String command = String.format(d, donation.player, donation.amount, new Date(donation.contributed), donation.getKey());
            this.getCoordinator().plugin.getLogger().finest("Dispatching command: " + command);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
        }
        this.getCoordinator().savePending();
    }

    public void remove(final Donation donation) {
        this.pending.remove(donation);
        for (final Trigger trigger : this.triggers) trigger.remove(donation);
    }

    public Coordinator getCoordinator() {
        return this.benefit.pkg.coordinator;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Command other = (Command) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public String getPath() {
        return this.benefit.getPath() + ".\"" + this.name + "\"";
    }

    @Override
    public String toString() {
        return "Command: [getPath(): " + this.getPath() + "; dispatch:" + this.dispatch + "; triggers: " + this.triggers + "]";
    }

}
