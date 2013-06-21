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

    /** lower case package name, Package */
    public final Map<String, Package> packages = new HashMap<String, Package>();

    /** donation key, Donation */
    public final Map<String, Donation> donations = new HashMap<String, Donation>();

    /** lower case donation origin, lower case player name */
    public final Map<String, String> registrations = new HashMap<String, String>();

    public final Plugin plugin;
    public final String currency;

    private boolean sandbox = false;

    Coordinator(final Plugin plugin, final String currency) {
        this.plugin = plugin;
        this.currency = currency;
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
        this.registrations.clear();
    }

    Package putPackage(final Package pkg) {
        return this.packages.put(pkg.name.toLowerCase(), pkg);
    }

    /** @param name lower case Package name */
    public Package getPackage(final String name) {
        return this.packages.get(name);
    }

    Donation putDonation(final Donation donation) {
        return this.donations.put(donation.getKey(), donation);
    }

    public Donation getDonation(final String key) {
        return this.donations.get(key);
    }

    public String putRegistration(final String origin, final String player) {
        final String previous = this.registrations.put(origin, player);
        if (previous != null) ((Main) this.plugin).deleteRegistration(origin, previous);
        ((Main) this.plugin).saveRegistration(origin, player);

        for (final Donation donation : this.unassigned(origin)) {
            final Donation updated = donation.register(player);
            this.putDonation(updated);
            this.assign(updated);
        }

        return previous;
    }

    public void assign(final Donation donation) {
        this.plugin.getLogger().log(( this.sandbox ? Level.INFO : Level.FINEST ), "{0,choice,1#|[Sandbox] }Donation: {1}", new Object[] { this.sandbox?1:0, donation });
        if (!this.sandbox) this.donations.put(donation.getKey(), donation);

        // do not attempt assignment if player not identified yet
        if (donation.player == null) {
            this.plugin.getLogger().log(( this.sandbox ? Level.INFO : Level.FINEST ), "{0,choice,1#|[Sandbox] }No packages assigned; Player not identified for origin: {1}", new Object[] { this.sandbox?1:0, donation.origin });
            if (!this.sandbox) ((Main) this.plugin).saveDonation(donation);
            return;
        }

        donation.packages = new ArrayList<String>();
        for (final Package pkg : this.applicable(donation)) {
            if (!this.sandbox) donation.packages.add(pkg.name);
            pkg.assign(donation);
        }

        if (!this.sandbox) {
            ((Main) this.plugin).saveDonation(donation);
            this.savePending();
        }
    }

    /** unassigned donations for a given player ordered by oldest contribution first */
    public List<Donation> unassigned(final String origin) {
        final String lower = origin.toLowerCase();

        final List<Donation> result = new ArrayList<Donation>();
        for (final Donation donation : this.donations.values()) {
            if (donation.player == null && donation.origin.toLowerCase().equals(lower) && donation.packages == null)
                result.add(donation);
        }

        Collections.sort(result, Collections.reverseOrder(Donation.NEWEST_CONTRIBUTION_FIRST));
        return result;
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
