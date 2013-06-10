package edgruberman.bukkit.donations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.Plugin;

/** manages benefit distribution */
public final class Coordinator {

    /** Package name, Package */
    public final Map<String, Package> packages = new HashMap<String, Package>();

    /** Donation key, Donation */
    public final Map<String, Donation> assigned = new HashMap<String, Donation>();

    public final Plugin plugin;

    Coordinator(final Plugin plugin) {
        this.plugin = plugin;
    }

    void clear() {
        for (final Package pkg : this.packages.values()) pkg.clear();
        this.packages.clear();
        this.assigned.clear();
    }

    void addPackage(final Package pkg) {
        this.packages.put(pkg.name.toLowerCase(), pkg);
    }

    /** @param name lower case Package name */
    public Package getPackage(final String name) {
        return this.packages.get(name);
    }

    void addAssigned(final Donation donation) {
        this.assigned.put(donation.getKey(), donation);
    }

    public Donation getAssigned(final String key) {
        return this.assigned.get(key);
    }

    public void assign(final Donation donation) {
        // TODO sandbox mode
        // TODO logging

        // ensure no duplicate processing even if a problem occurs
        this.assigned.put(donation.getKey(), donation);

        for (final Package pkg : this.applicable(donation)) {
            donation.packages.add(pkg.name);
            for (final Benefit benefit : pkg.benefits.values())
                for (final Command command : benefit.commands.values())
                    command.add(donation);
        }

        ((Main) this.plugin).saveAssigned(donation);
        this.savePending();
    }

    // TODO include pending
    /** assigned donations for a given player ordered by newest contribution first */
    public List<Donation> history(final String player) {
        final List<Donation> history = new ArrayList<Donation>();
        for (final Donation donation : this.assigned.values())
            if (donation.player.equalsIgnoreCase(player))
                history.add(donation);

        Collections.sort(history, Donation.NEWEST_CONTRIBUTION_FIRST);
        return history;
    }

    /** packages to be applied for a given donation amount ordered by lowest minimum first */
    public List<Package> applicable(final Donation donation) {
        final List<Package> result = new ArrayList<Package>();
        for (final Package pkg : this.packages.values())
            if (pkg.applicable(donation))
                result.add(pkg);

        Collections.sort(result, Package.LOWEST_MINIMUM_FIRST);
        return result;
    }

    /** @return milliseconds since epoch of most recent time package was applicable to player */
    public Long last(final Package pkg, final String player) {
        Long result = null;

        for (final Donation previous : this.history(player))
            if (previous.packages.contains(pkg.name)) {
                result = previous.contributed;
                break;
            }

        return result;
    }

    public void savePending() {
        ((Main) this.plugin).savePending();
    }

}
