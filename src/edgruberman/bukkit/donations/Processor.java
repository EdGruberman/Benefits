package edgruberman.bukkit.donations;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.configuration.ConfigurationSection;

/** payment processor, responsible for monitoring for verifying payments, avoiding duplicate processing, generating Donation, and then passing to coordinator for package distribution */
public abstract class Processor {

    // ---- static factory ----

    public static final String DEFAULT_PACKAGE = Processor.class.getPackage().getName() + ".processors";

    public static Processor create(final String className, final Coordinator coordinator, final ConfigurationSection config)
            throws ClassNotFoundException, ClassCastException, InstantiationException, IllegalAccessException
            , SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
            {
        final Class<? extends Processor> subClass = Processor.find(className);
        final Constructor<? extends Processor> ctr = subClass.getConstructor(Coordinator.class, ConfigurationSection.class);
        return ctr.newInstance(coordinator, config);
    }

    public static Class<? extends Processor> find(final String className) throws ClassNotFoundException, ClassCastException {
        try {
            // look in default package first
            return Class.forName(Processor.DEFAULT_PACKAGE + "." + className).asSubclass(Processor.class);

        } catch (final Exception e) {
            // ignore to try searching for custom class next
        }

        // look for a custom class
        return Class.forName(className).asSubclass(Processor.class);
    }



    // ---- instance ----

    protected Coordinator coordinator;

    protected Processor(final Coordinator coordinator, final ConfigurationSection config) {
        this.coordinator = coordinator;
    }

    protected Donation process(final String id, final String origin, final String player, final long amount, final long contributed) {
        final String registration = ( player != null ? player : this.coordinator.registrations.get(origin.toLowerCase()) );
        final Donation donation = new Donation(this.getId(), id, origin, registration, amount, contributed, null);
        this.coordinator.assign(donation);
        return donation;
    }

    public String getId() {
        return this.getClass().getSimpleName();
    }

    /** called to cease any further processing */
    public void stop() {}

}
