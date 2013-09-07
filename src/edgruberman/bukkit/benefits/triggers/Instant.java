package edgruberman.bukkit.benefits.triggers;

import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.benefits.Command;
import edgruberman.bukkit.benefits.Contribution;
import edgruberman.bukkit.benefits.Trigger;

public class Instant extends Trigger {

    public Instant(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @Override
    public void add(final Contribution contribution) {
        this.command.dispatch(contribution);
    }

}
