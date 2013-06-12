package edgruberman.bukkit.donations.triggers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Trigger;

public abstract class PlayerEvent extends Trigger implements Listener {

    /** lower case player name, donations */
    protected final Map<String, List<Donation>> pending = new HashMap<String, List<Donation>>();

    private boolean registered = false;

    protected PlayerEvent(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @Override
    public Collection<Donation> getPending() {
        final Collection<Donation> result = new ArrayList<Donation>();

        for (final List<Donation> l : this.pending.values())
            for (final Donation d : l)
                result.add(d);

        return result;
    }

    @Override
    public void clear() {
        HandlerList.unregisterAll(this);
        this.pending.clear();
    }

    @Override
    public void add(final Donation donation) {
        final String playerName = donation.player.toLowerCase();
        if (!this.pending.containsKey(playerName)) this.pending.put(playerName, new ArrayList<Donation>());
        this.pending.get(playerName).add(donation);
        if (this.registered) return;

        Bukkit.getPluginManager().registerEvents(this, this.command.getCoordinator().plugin);
        this.registered = true;
    }

    @Override
    public boolean remove(final Donation donation) {
        final String name = donation.player.toLowerCase();
        final List<Donation> donations = this.pending.get(name);
        if (donations == null) return false;

        boolean removed = false;
        final Iterator<Donation> itDonations = donations.iterator();
        while (itDonations.hasNext()) {
            final Donation pending = itDonations.next();
            if (pending.equals(donation)) {
                itDonations.remove();
                removed = true;
                break;
            }
        }

        if (donations.size() == 0) this.pending.remove(name);
        if (this.pending.size() == 0) this.clear();
        return removed;
    }

    protected void dispatch(final Player player) {
        final String name = player.getName().toLowerCase();
        final List<Donation> donations = this.pending.get(name);
        if (donations == null) return;

        final Iterator<Donation> itDonations = donations.iterator();
        while (itDonations.hasNext()) {
            final Donation donation = itDonations.next();
            this.command.dispatch(donation);
            itDonations.remove();
        }

        if (donations.size() == 0) this.pending.remove(name);
        if (this.pending.size() == 0) this.clear();
    }

}
