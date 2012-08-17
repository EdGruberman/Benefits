package edgruberman.bukkit.donations.triggers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.Donation;

public abstract class PlayerEvent extends Trigger implements Listener {

    protected final Map<String, List<Donation>> donations = new HashMap<String, List<Donation>>();

    private boolean registered = false;

    protected PlayerEvent(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @Override
    public void clear() {
        HandlerList.unregisterAll(this);
        this.donations.clear();
    }

    @Override
    public void add(final Donation donation) {
        final String playerName = donation.player.toLowerCase();
        if (!this.donations.containsKey(playerName)) this.donations.put(playerName, new ArrayList<Donation>());
        this.donations.get(playerName).add(donation);
        if (this.registered) return;

        Bukkit.getPluginManager().registerEvents(this, this.command.getCoordinator().plugin);
        this.registered = true;
    }

    protected void dispatch(final Player player) {
        for (final Donation donation : this.donations.get(player.getName().toLowerCase()))
            this.command.dispatch(donation);
    }

    @Override
    public void remove(final Donation donation) {
        this.donations.remove(donation.player.toLowerCase());
    }

}
