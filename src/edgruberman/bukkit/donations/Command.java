package edgruberman.bukkit.donations;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

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
        this.dispatch.addAll(Command.getStringList(definition, "dispatch"));
        this.undo.addAll(Command.getStringList(definition, "undo"));

        for (final String triggerClass : Command.getStringList(definition, "triggers")) {
            Trigger trigger;
            try {
                trigger = Trigger.create(triggerClass, this, definition);
            } catch (final Exception e) {
                this.benefit.pkg.coordinator.plugin.getLogger().log(Level.WARNING, "Failed to create Trigger: {0}; {1}", new Object[] { triggerClass, e });
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
            this.getCoordinator().plugin.getLogger().log(Level.FINEST, "  Trigger added for {0} to {1}", new Object[] { donation.getKey(), trigger.getPath() });
            trigger.add(donation);
        }
    }

    public boolean dispatch(final Donation donation) {
        if (!this.remove(donation)) return false;
        this.execute(donation, this.dispatch);
        this.getCoordinator().savePending();
        return true;
    }

    public boolean undo(final Donation donation) {
        if (!this.remove(donation)) return false;
        this.execute(donation, this.undo);
        this.getCoordinator().savePending();
        return true;
    }

    private void execute(final Donation donation, final Collection<String> commands) {
        for (final String d : commands) {
            String command;
            try {
                command = MessageFormat.format(d, donation.player, donation.currency, donation.amount / 100D, (int) Math.floor(donation.amount / 100D), new Date(donation.contributed), donation.getKey());
            } catch (final Exception e) {
                this.getCoordinator().plugin.getLogger().log(Level.WARNING, "Exception parsing command for {0}; {1}; {2}", new Object[] { this.getPath(), e, d });
                continue;
            }

            this.getCoordinator().plugin.getLogger().finest("Executing command: " + command);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
        }
    }

    private boolean remove(final Donation donation) {
        boolean removed = false;
        for (final Trigger trigger : this.triggers) {
            if (trigger.remove(donation)) removed = true;
        }
        return removed;
    }

    public Coordinator getCoordinator() {
        return this.benefit.pkg.coordinator;
    }

    public String getPath() {
        return this.benefit.getPath() + ">\"" + this.name + "\"";
    }

    @Override
    public String toString() {
        return "Command: [getPath(): " + this.getPath() + "; dispatch:" + this.dispatch + "; triggers: " + this.triggers + "]";
    }



    private static List<String> getStringList(final ConfigurationSection config, final String path) {
        if (config.isList(path))
            return config.getStringList(path);

        if (config.isString(path))
            return Arrays.asList(config.getString(path));

        return Collections.emptyList();
    }

}
