package edgruberman.bukkit.donations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/** Record of money being granted */
public final class Donation {

    /** Source of donation, usually an e-mail address */
    public final String origin;

    /** In-game name of player to apply donation benefits to */
    public final String player;

    /** Financial total donated (USD) */
    public final double amount;

    /** When donation was made in milliSeconds from midnight, January 1, 1970 UTC */
    public final Long contributed;

    /** Package names applied to donation */
    public Collection<String> packages = new ArrayList<String>();

    public Donation(final String origin, final String player, final double amount, final long contributed, final Collection<String> packages) {
        this.origin = origin;
        this.player = player;
        this.amount = amount;
        this.contributed = contributed;
        if (packages != null) this.packages.addAll(packages);
    }

    /** <Player>-<Origin>-<Contributed> */
    public String getKey() {
        return this.player.toLowerCase() + "-" + this.origin.replaceAll("\\.", "_").toLowerCase() + "-" + this.contributed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.contributed ^ (this.contributed >>> 32));
        result = prime * result + ((this.origin == null) ? 0 : this.origin.hashCode());
        result = prime * result
                + ((this.player == null) ? 0 : this.player.hashCode());
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
        final Donation other = (Donation) obj;
        if (this.contributed != other.contributed) {
            return false;
        }
        if (this.origin == null) {
            if (other.origin != null) {
                return false;
            }
        } else if (!this.origin.equals(other.origin)) {
            return false;
        }
        if (this.player == null) {
            if (other.player != null) {
                return false;
            }
        } else if (!this.player.equals(other.player)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Donation [origin=" + this.origin + ", playerName=" + this.player + ", amount=" + this.amount + ", contributed=" + this.contributed + "]";
    }



    public static final NewestContributionFirst NEWEST_CONTRIBUTION_FIRST = new Donation.NewestContributionFirst();

    private static class NewestContributionFirst implements Comparator<Donation> {

        @Override
        public int compare(final Donation x, final Donation y) {
            return y.contributed.compareTo(x.contributed);
        }

    }

}
