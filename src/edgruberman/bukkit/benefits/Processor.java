package edgruberman.bukkit.benefits;

import org.bukkit.configuration.ConfigurationSection;

/** payment processor, responsible for monitoring for verifying payments, avoiding duplicate processing, generating Contribution, and then passing to coordinator for package distribution */
public abstract class Processor {

    protected Coordinator coordinator;

    protected Processor(final Coordinator coordinator, final ConfigurationSection config) {
        this.coordinator = coordinator;
    }

    protected Contribution process(final String id, final String origin, final String player, final String currency, final long amount, final long contributed) {
        final String registration = ( player != null ? player : this.coordinator.registrations.get(origin.toLowerCase()) );
        final Contribution contribution = new Contribution(this.getId(), id, origin, registration, currency, amount, contributed, null);
        this.coordinator.assign(contribution);
        return contribution;
    }

    public String getId() {
        return this.getClass().getSimpleName();
    }

    /** called to cease any further processing */
    public void stop() {}

}
