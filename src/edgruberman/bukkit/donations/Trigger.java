package edgruberman.bukkit.donations;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;

import org.bukkit.configuration.ConfigurationSection;

/** event manager monitors for any associated donation */
public abstract class Trigger {

    // ---- static factory ----

    public static final String DEFAULT_PACKAGE = Trigger.class.getPackage().getName() + ".triggers";

    public static Trigger create(final String className, final Command command, final ConfigurationSection definition)
            throws ClassNotFoundException, ClassCastException, InstantiationException, IllegalAccessException
            , SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
            {
        final Class<? extends Trigger> subClass = Trigger.find(className);
        final Constructor<? extends Trigger> ctr = subClass.getConstructor(Command.class, ConfigurationSection.class);
        return ctr.newInstance(command, definition);
    }

    public static Class<? extends Trigger> find(final String className) throws ClassNotFoundException, ClassCastException {
        try {
            // look in default package first
            return Class.forName(Trigger.DEFAULT_PACKAGE + "." + className).asSubclass(Trigger.class);

        } catch (final Exception e) {
            // ignore to try searching for custom class next
        }

        // look for a custom class
        return Class.forName(className).asSubclass(Trigger.class);
    }



    // ---- instance ----

    protected final Command command;

    protected Trigger(final Command command, final ConfigurationSection definition) {
        this.command = command;
    }

    /** current queue for donations waiting for trigger conditions to be met */
    public Collection<Donation> getPending() {
        return Collections.emptyList();
    }

    /** prepare a trigger to be fired for a donation */
    public abstract void add(final Donation donation);

    /** prevent a trigger from firing in the future for a donation */
    public boolean remove(final Donation donation) { return true; };

    /** perform finalizing cleanup */
    public void clear() {};

    public String getName() {
        return (this.getClass().getPackage().equals(Trigger.class.getPackage()) ? this.getClass().getSimpleName() : this.getClass().getName());
    }

    public String getPath() {
        return this.command.getPath() + ">\"" + this.getName() + "\"";
    }

    @Override
    public String toString() {
        return this.toString("");
    }

    protected String toString(final String custom) {
        return String.format("Trigger (%1$s): [%2$s]", this.getName(), custom);
    }

}
