package edgruberman.bukkit.donations;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public final class Command {

    public final Benefit benefit;
    public final String name;
    public final Collection<String> dispatch = new ArrayList<String>();
    public final Collection<String> undo = new ArrayList<String>();
    public final Collection<Trigger> triggers = new HashSet<Trigger>();

    Command(final Benefit benefit, final ConfigurationSection definition) {
        this.benefit = benefit;
        this.name = definition.getName();
        this.dispatch.addAll(Coordinator.getStringList(definition, "dispatch"));
        this.undo.addAll(Coordinator.getStringList(definition, "undo"));

        for (final String triggerClass : Coordinator.getStringList(definition, "triggers")) {
            Trigger trigger;
            try {
                trigger = Trigger.create(triggerClass, this, definition);
            } catch (final Exception e) {
                this.benefit.pkg.coordinator.plugin.getLogger().warning("Failed to create Trigger: " + triggerClass + "; " + e.getClass().getName() + ": " + e.getMessage());
                continue;
            }
            this.triggers.add(trigger);
        }
    }

    void clear() {
        for (final Trigger trigger : this.triggers) trigger.clear();
        this.triggers.clear();
    }

    public void add(final Donation donation) {
        for (final Trigger trigger : this.triggers) {
            this.getCoordinator().plugin.getLogger().finest("Adding " + donation.toString() + " to " + trigger.getPath());
            trigger.add(donation);
        }
    }

    public void dispatch(final Donation donation) {
        this.remove(donation);
        this.execute(donation, this.dispatch);
        this.getCoordinator().savePending();
    }

    public void undo(final Donation donation) {
        this.remove(donation);
        this.execute(donation, this.undo);
        this.getCoordinator().savePending();
    }

    private void execute(final Donation donation, final Collection<String> commands) {
        for (final String d : commands) {
            final String command = MessageFormat.format(d, donation.player, donation.amount, new Date(donation.contributed), donation.getKey());
            this.getCoordinator().plugin.getLogger().finest("Executing command: " + command);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
        }
    }

    private void remove(final Donation donation) {
        for (final Trigger trigger : this.triggers)
            trigger.remove(donation);
    }

    public Coordinator getCoordinator() {
        return this.benefit.pkg.coordinator;
    }

    public String getPath() {
        return this.benefit.getPath() + ".\"" + this.name + "\"";
    }

    @Override
    public String toString() {
        return "Command: [getPath(): " + this.getPath() + "; dispatch:" + this.dispatch + "; triggers: " + this.triggers + "]";
    }

}
