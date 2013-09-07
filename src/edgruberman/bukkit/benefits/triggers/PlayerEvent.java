package edgruberman.bukkit.benefits.triggers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import edgruberman.bukkit.benefits.Command;
import edgruberman.bukkit.benefits.Contribution;
import edgruberman.bukkit.benefits.Trigger;

public abstract class PlayerEvent extends Trigger implements Listener {

    /** lower case player name, contributions */
    protected final Map<String, List<Contribution>> pending = new HashMap<String, List<Contribution>>();

    private boolean registered = false;

    protected PlayerEvent(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @Override
    public Collection<Contribution> getPending() {
        final Collection<Contribution> result = new ArrayList<Contribution>();

        for (final List<Contribution> l : this.pending.values())
            for (final Contribution d : l)
                result.add(d);

        return result;
    }

    @Override
    public void clear() {
        HandlerList.unregisterAll(this);
        this.pending.clear();
    }

    @Override
    public void add(final Contribution contribution) {
        final String playerName = contribution.player.toLowerCase();
        if (!this.pending.containsKey(playerName)) this.pending.put(playerName, new ArrayList<Contribution>());
        this.pending.get(playerName).add(contribution);
        if (this.registered) return;

        Bukkit.getPluginManager().registerEvents(this, this.command.getCoordinator().plugin);
        this.registered = true;
    }

    @Override
    public boolean remove(final Contribution contribution) {
        final String name = contribution.player.toLowerCase();
        final List<Contribution> contributions = this.pending.get(name);
        if (contributions == null) return false;

        boolean removed = false;
        final Iterator<Contribution> itContributions = contributions.iterator();
        while (itContributions.hasNext()) {
            final Contribution pending = itContributions.next();
            if (pending.equals(contribution)) {
                itContributions.remove();
                removed = true;
                break;
            }
        }

        if (contributions.size() == 0) this.pending.remove(name);
        if (this.pending.size() == 0) this.clear();
        return removed;
    }

    protected void apply(final OfflinePlayer player) {
        final String name = player.getName().toLowerCase();
        final List<Contribution> contributions = this.pending.get(name);
        if (contributions == null) return;

        // make a copy to allow for command dispatch to remove contribution from trigger during processing
        final List<Contribution> copy = new ArrayList<Contribution>(contributions);
        for (final Contribution contribution : copy) this.command.dispatch(contribution);
    }

}
