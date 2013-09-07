package edgruberman.bukkit.benefits;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;

/** record of money being granted */
public final class Contribution {

    public static String createKey(final String processor, final String id) {
        return processor + "-" + id;
    }



    /** unique payment processor identifier */
    public final String processor;

    /** payment processor specific unique identifier */
    public final String id;

    /** source of contribution, usually an e-mail address */
    public final String origin;

    /** in-game name of player to apply contribution benefits to, null if not registered */
    public final String player;

    /** financial denomination of amount */
    public final String currency;

    /** financial total contributed multiplied by 100 */
    public final long amount;

    /** when contribution was made in milliSeconds from midnight, January 1, 1970 UTC */
    public final Long contributed;

    /** package names applied to contribution */
    public Collection<String> packages;

    private final String key;

    /** new, unassigned, incoming contribution */
    public Contribution(final Processor processor, final String id, final String origin, final String player, final String currency, final long amount, final long contributed) {
        this(processor.getId(), id, origin, player, currency, amount, contributed, null);
    }

    /** existing contribution with packages already applied */
    public Contribution(final String processor, final String id, final String origin, final String player, final String currency, final long amount, final long contributed, final Collection<String> packages) {
        this.processor = processor;
        this.id = id;
        this.origin = origin;
        this.player = player;
        this.currency = currency;
        this.amount = amount;
        this.contributed = contributed;
        this.packages = packages;

        this.key = Contribution.createKey(processor, id);
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
        if (!(obj instanceof Contribution)) return false;
        final Contribution other = (Contribution) obj;
        return this.key.equals(other.key);
    }

    @Override
    public String toString() {
        return MessageFormat.format("Contribution [processor={0}, id={1}, origin={2}, player={3}, currency={4}, amount={5,number,0.00}, contributed={6,date,yyyy MMM dd hh:mm aa}]"
                , this.processor, this.id, this.origin, this.player, this.currency, this.amount / 100D, this.contributed);
    }

    Contribution register(final String player) {
        return new Contribution(this.processor, this.id, this.origin, player, this.currency, this.amount, this.contributed, this.packages);
    }


    public static final NewestContributionFirst NEWEST_CONTRIBUTION_FIRST = new Contribution.NewestContributionFirst();

    private static class NewestContributionFirst implements Comparator<Contribution> {

        @Override
        public int compare(final Contribution x, final Contribution y) {
            return y.contributed.compareTo(x.contributed);
        }

    }

}
