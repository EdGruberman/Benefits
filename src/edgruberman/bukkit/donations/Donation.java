package edgruberman.bukkit.donations;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;

/** record of money being granted */
public final class Donation {

    public static String createKey(final String processor, final String id) {
        return processor + ":" + id;
    }



    /** unique payment processor identifier */
    public final String processor;

    /** payment processor specific unique identifier */
    public final String id;

    /** source of donation, usually an e-mail address */
    public final String origin;

    /** in-game name of player to apply donation benefits to */
    public final String player;

    /** financial total donated (USD) */
    public final long amount;

    /** when donation was made in milliSeconds from midnight, January 1, 1970 UTC */
    public final Long contributed;

    /** package names applied to donation */
    public Collection<String> packages;

    private final String key;

    /** new, unassigned, incoming donation */
    public Donation(final Processor processor, final String id, final String origin, final String player, final long amount, final long contributed) {
        this(processor.getId(), id, origin, player, amount, contributed, null);
    }

    /** existing donation with packages already applied */
    public Donation(final String processor, final String id, final String origin, final String player, final long amount, final long contributed, final Collection<String> packages) {
        this.processor = processor;
        this.id = id;
        this.origin = origin;
        this.player = player;
        this.amount = amount;
        this.contributed = contributed;
        this.packages = packages;

        this.key = Donation.createKey(processor, id);
    }

    /** (Processor)-(ID) */
    public String getKey() {
        return this.key;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Donation)) return false;
        final Donation other = (Donation) obj;
        return this.key.equals(other.key);
    }

    @Override
    public String toString() {
        return MessageFormat.format("Donation [processor={0},id={1},origin={2}, playerName={3}, amount={4}, contributed={5}]"
                , this.processor, this.id, this.origin, this.player, this.amount, this.contributed);
    }

    public Donation as(final String player) {
        return new Donation(this.processor, this.id, this.origin, player, this.amount, this.contributed, this.packages);
    }



    public static final NewestContributionFirst NEWEST_CONTRIBUTION_FIRST = new Donation.NewestContributionFirst();

    private static class NewestContributionFirst implements Comparator<Donation> {

        @Override
        public int compare(final Donation x, final Donation y) {
            return y.contributed.compareTo(x.contributed);
        }

    }

}
