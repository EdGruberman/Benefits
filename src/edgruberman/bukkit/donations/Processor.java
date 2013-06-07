package edgruberman.bukkit.donations;

/** payment processor, responsible for monitoring for verifying payments, avoiding duplicate processing, generating Donation, and then passing to coordinator for package distribution */
public abstract class Processor {

    protected Coordinator coordinator;

    protected Processor(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    protected Donation process(final String id, final String origin, final String player, final double amount, final long contributed) {
        final Donation donation = new Donation(this.getClass().getName(), id, origin, player, amount, contributed, null);
        this.coordinator.distribute(donation);
        return donation;
    }

}
