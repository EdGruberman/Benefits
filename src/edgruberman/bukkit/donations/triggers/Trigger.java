package edgruberman.bukkit.donations.triggers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.Donation;

public abstract class Trigger {

    // ---- Static Factory ----

    public static Trigger create(final String className, final Command command, final ConfigurationSection definition) throws ClassNotFoundException, ClassCastException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        final Class<? extends Trigger> subClass = Trigger.find(className);
        final Constructor<? extends Trigger> ctr = subClass.getConstructor(Command.class, ConfigurationSection.class);
        return ctr.newInstance(command, definition);
    }

    public static Class<? extends Trigger> find(final String className) throws ClassNotFoundException, ClassCastException {
        // Look in local package first
        try {
            return Class.forName(Trigger.class.getPackage().getName() + "." + className).asSubclass(Trigger.class);
        } catch (final Exception e) {
            // Ignore to try searching for custom class next
        }

        // Look for a custom class
        return Class.forName(className).asSubclass(Trigger.class);
    }



    // ---- Instance ----

    public final Command command;

    protected Trigger(final Command command, final ConfigurationSection definition) {
        this.command = command;
    }

    /** Prepare a trigger to be fired for a processed donation */
    public abstract void add(final Donation donation);

    /** Prevent a trigger from firing in the future for a donation */
    public void remove(final Donation donation) {};

    public void clear() {};

    public String getName() {
        return (this.getClass().getPackage().equals(Trigger.class.getPackage()) ? this.getClass().getSimpleName() : this.getClass().getName());
    }

    public String getPath() {
        return this.command.getPath() + ".\"" + this.getName() + "\"";
    }

    @Override
    public String toString() {
        return this.toString("");
    }

    protected String toString(final String custom) {
        return String.format("Trigger (%1$s): [%2$s]", this.getName(), custom);
    }

}
