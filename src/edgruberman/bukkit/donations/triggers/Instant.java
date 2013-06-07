package edgruberman.bukkit.donations.triggers;

import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Trigger;

public class Instant extends Trigger {

    public Instant(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @Override
    public void add(final Donation donation) {
        this.command.dispatch(donation);
    }

}
