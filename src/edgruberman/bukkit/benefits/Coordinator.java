package edgruberman.bukkit.benefits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

/** manages benefit distribution */
public final class Coordinator {

    /** lower case package name, Package */
    public final Map<String, Package> packages = new HashMap<String, Package>();

    /** contribution key, Contribution */
    public final Map<String, Contribution> contributions = new HashMap<String, Contribution>();

    /** lower case contribution origin, lower case player name */
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
        this.contributions.clear();
        this.registrations.clear();
    }

    Package putPackage(final Package pkg) {
        return this.packages.put(pkg.name.toLowerCase(), pkg);
    }

    /** @param name lower case Package name */
    public Package getPackage(final String name) {
        return this.packages.get(name);
    }

    Contribution putContribution(final Contribution contribution) {
        return this.contributions.put(contribution.getKey(), contribution);
    }

    public Contribution getContribution(final String key) {
        return this.contributions.get(key);
    }

    public String putRegistration(final String origin, final String player) {
        final String previous = this.registrations.put(origin, player);
        if (previous != null) ((Main) this.plugin).deleteRegistration(origin, previous);
        ((Main) this.plugin).saveRegistration(origin, player);

        for (final Contribution contribution : this.unassigned(origin)) {
            final Contribution updated = contribution.register(player);
            this.putContribution(updated);
            this.assign(updated);
        }

        return previous;
    }

    public void assign(final Contribution contribution) {
        this.plugin.getLogger().log(( this.sandbox ? Level.INFO : Level.FINEST ), "{0,choice,1#|[Sandbox] }Contribution: {1}", new Object[] { this.sandbox?1:0, contribution });
        if (!this.sandbox) this.contributions.put(contribution.getKey(), contribution);

        // do not attempt assignment if player not identified yet
        if (contribution.player == null) {
            this.plugin.getLogger().log(( this.sandbox ? Level.INFO : Level.FINEST ), "{0,choice,1#|[Sandbox] }No packages assigned; Player not identified for origin: {1}", new Object[] { this.sandbox?1:0, contribution.origin });
            if (!this.sandbox) ((Main) this.plugin).saveContribution(contribution);
            return;
        }

        final OfflinePlayer contributor = this.plugin.getServer().getOfflinePlayer(contribution.player);
        final ContributionEvent event = new ContributionEvent(contributor, new Date(contribution.contributed), contribution.currency, contribution.amount);
        this.plugin.getServer().getPluginManager().callEvent(event);

        contribution.packages = new ArrayList<String>();
        for (final Package pkg : this.applicable(contribution)) {
            if (!this.sandbox) contribution.packages.add(pkg.name);
            pkg.assign(contribution);
        }

        if (!this.sandbox) {
            ((Main) this.plugin).saveContribution(contribution);
            this.savePending();
        }
    }

    /** unassigned contributions for a given player ordered by oldest contribution first */
    public List<Contribution> unassigned(final String origin) {
        final String lower = origin.toLowerCase();

        final List<Contribution> result = new ArrayList<Contribution>();
        for (final Contribution contribution : this.contributions.values()) {
            if (contribution.player == null && contribution.origin.toLowerCase().equals(lower) && contribution.packages == null)
                result.add(contribution);
        }

        Collections.sort(result, Collections.reverseOrder(Contribution.NEWEST_CONTRIBUTION_FIRST));
        return result;
    }

    /** assigned contributions for a given player ordered by newest contribution first */
    public List<Contribution> history(final String player) {
        final String lower = player.toLowerCase();

        final List<Contribution> result = new ArrayList<Contribution>();
        for (final Contribution contribution : this.contributions.values())
            if (contribution.player != null && contribution.player.toLowerCase().equals(lower))
                result.add(contribution);

        Collections.sort(result, Contribution.NEWEST_CONTRIBUTION_FIRST);
        return result;
    }

    /** packages to be applied for a given contribution amount ordered by lowest minimum first */
    public List<Package> applicable(final Contribution contribution) {
        final List<Package> result = new ArrayList<Package>();
        for (final Package pkg : this.packages.values())
            if (pkg.applicable(contribution))
                result.add(pkg);

        Collections.sort(result, Package.LOWEST_MINIMUM_FIRST);
        return result;
    }

    /** @return milliseconds since epoch of most recent time package was applicable to player */
    public Long last(final Package pkg, final String player) {
        Long result = null;

        for (final Contribution previous : this.history(player))
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
