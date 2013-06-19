package edgruberman.bukkit.donations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

/** collection of Benefits */
public final class Package {

    private static final long DEFAULT_MINIMUM = -1; // no minimum
    private static final long DEFAULT_LIMIT = -1; // no limit

    private static final long LIMIT_TOLERANCE = 1000 * 60 * 60 * 12; // milliseconds of 12 hours

    public Coordinator coordinator;
    public String name;
    public String description;

    /** amount at which package is applicable for a donation */
    public long minimum;

    /** days before package can be applied for a new donation */
    public long limit;

    /** benefits index keyed on lower case benefit name */
    public Map<String, Benefit> benefits = new LinkedHashMap<String, Benefit>();

    Package(final Coordinator coordinator, final ConfigurationSection definition) {
        this.coordinator = coordinator;
        this.name = definition.getName();
        this.description = definition.getString("description");
        this.minimum = definition.getLong("minimum", Package.DEFAULT_MINIMUM);
        this.limit = definition.getLong("limit", Package.DEFAULT_LIMIT);

        final ConfigurationSection benefits = definition.getConfigurationSection("benefits");
        for (final String benefitName : benefits.getKeys(false)) {
            final Benefit benefit = new Benefit(this, benefits.getConfigurationSection(benefitName));
            this.benefits.put(benefit.name.toLowerCase(), benefit);
        }
    }

    void clear() {
        for (final Benefit benefit : this.benefits.values()) benefit.clear();
        this.benefits.clear();
    }

    public String getPath() {
        return "\"" + this.name + "\"";
    }

    @Override
    public String toString() {
        return this.getPath();
    }

    public boolean applicable(final Donation donation) {
        if (donation.amount < this.minimum) return false;

        // package is not applicable if last time package was applied is less than limit + tolerance
        if (this.limit <= 0) return true;
        final Long last = this.coordinator.last(this, donation.player);
        if (last == null) return true;

        final long since = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - last + Package.LIMIT_TOLERANCE);
        if (since < this.limit) {
            this.coordinator.plugin.getLogger().log(Level.FINER, "Package {0} applied only {1} day(s) ago (Limit: {2}) for {3}", new Object[] { this.name, since, this.limit, donation.player });
            return false;
        }

        return true;
    }

    public List<Command> assign(final Donation donation) {
        final List<Command> result = new ArrayList<Command>();
        for (final Benefit benefit : this.benefits.values()) {
            final List<Command> assigned = benefit.assign(donation);
            result.addAll(assigned);
        }
        return result;
    }



    public static final LowestMinimumFirst LOWEST_MINIMUM_FIRST = new Package.LowestMinimumFirst();

    private static final class LowestMinimumFirst implements Comparator<Package> {

        @Override
        public int compare(final Package x, final Package y) {
            return Long.valueOf(x.minimum).compareTo(Long.valueOf(y.minimum));
        }

    }

}
