package edgruberman.bukkit.donations.triggers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.Donation;

/** Immediately if player is currently connected, otherwise next time player joins */
public class Online extends Join {

    public Online(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @Override
    public void add(final Donation donation) {
        // trigger must be added first as dispatch verifies donation is assigned before dispatching
        super.add(donation);

        if (Bukkit.getServer().getPlayerExact(donation.player) != null) {
            this.command.dispatch(donation);
            return;
        }
    }

}
