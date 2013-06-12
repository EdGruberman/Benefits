package edgruberman.bukkit.donations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

/** manages benefit distribution */
public final class Coordinator {

    /** Package name, Package */
    public final Map<String, Package> packages = new HashMap<String, Package>();

    /** Donation key, Donation */
    public final Map<String, Donation> donations = new HashMap<String, Donation>();

    public final Plugin plugin;

    private boolean sandbox = false;

    Coordinator(final Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSandbox() {
        return this.sandbox;
    }

    public void setSandbox(final boolean enable) {
        this.sandbox = enable;
    }

    void clear() {
        for (final Package pkg : this.packages.values()) pkg.clear();
        this.packages.clear();
        this.donations.clear();
    }

    void addPackage(final Package pkg) {
        this.packages.put(pkg.name.toLowerCase(), pkg);
    }

    /** @param name lower case Package name */
    public Package getPackage(final String name) {
        return this.packages.get(name);
    }

    void addDonation(final Donation donation) {
        this.donations.put(donation.getKey(), donation);
    }

    public Donation getDonation(final String key) {
        return this.donations.get(key);
    }

    public void assign(final Donation donation) {
        this.plugin.getLogger().log(( this.sandbox ? Level.INFO : Level.FINEST ), "{0,choice,1#|[Sandbox] } Donation: {1}", new Object[] { this.sandbox?1:0, donation });
        if (!this.sandbox) this.donations.put(donation.getKey(), donation);

        final List<Package> applicable = this.applicable(donation);

        final List<String> assigned = new ArrayList<String>();
        for (final Package pkg : applicable) {
            assigned.add(pkg.name);
            for (final Benefit benefit : pkg.benefits.values()) {
                for (final Command command : benefit.commands.values()) {
                    if (!this.sandbox) command.add(donation);
                    this.plugin.getLogger().log(( this.sandbox ? Level.INFO : Level.FINEST ), "{0,choice,1#|[Sandbox] } {0,choice,0#Assigned|1#Applicable}: {1}"
                            , new Object[] { this.sandbox?1:0, pkg, command });
                }
            }
        }

        if (this.sandbox) return;

        donation.packages = assigned;
        ((Main) this.plugin).saveAssigned(donation);
        this.savePending();
    }

    /** assigned donations for a given player ordered by newest contribution first */
    public List<Donation> history(final String player) {
        final String lower = player.toLowerCase();

        final List<Donation> result = new ArrayList<Donation>();
        for (final Donation donation : this.donations.values())
            if (donation.player != null && donation.player.toLowerCase().equals(lower))
                result.add(donation);

        Collections.sort(result, Donation.NEWEST_CONTRIBUTION_FIRST);
        return result;
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
