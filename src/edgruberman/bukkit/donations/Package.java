package edgruberman.bukkit.donations;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

public final class Package {

    public Coordinator coordinator;
    public String name;
    public String description;

    /** Amount at which package is applicable for a donation */
    public Double minimum;

    /** Days before package can be applied for a new donation */
    public Integer limit;

    /** Benefits index keyed on lower case benefit name */
    public Map<String, Benefit> benefits = new LinkedHashMap<String, Benefit>();

    Package(final Coordinator coordinator, final ConfigurationSection definition) {
        this.coordinator = coordinator;
        this.name = definition.getName();
        this.description = definition.getString("description");
        this.minimum = definition.getDouble("minimum");
        this.limit = definition.getInt("limit");

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



    public static final LowestMinimumFirst LOWEST_MINIMUM_FIRST = new Package.LowestMinimumFirst();

    private static final class LowestMinimumFirst implements Comparator<Package> {

        @Override
        public int compare(final Package x, final Package y) {
            return x.minimum.compareTo(y.minimum);
        }

    }

}
