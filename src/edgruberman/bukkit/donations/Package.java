package edgruberman.bukkit.donations;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

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

    Package(final Coordinator coordinator, final String name, final String description, final double minimum, final Integer limit) {
        this.coordinator = coordinator;
        this.name = name;
        this.description = description;
        this.minimum = minimum;
        this.limit = limit;
    }

    void clear() {
        for (final Benefit benefit : this.benefits.values()) benefit.clear();
        this.benefits.clear();
    }

    public String getPath() {
        return "\"" + this.name + "\"";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
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
        final Package other = (Package) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }



    public static final LowestMinimumFirst LOWEST_MINIMUM_FIRST = new Package.LowestMinimumFirst();

    private static final class LowestMinimumFirst implements Comparator<Package> {

        @Override
        public int compare(final Package x, final Package y) {
            return x.minimum.compareTo(y.minimum);
        }

    }

}
