package edgruberman.bukkit.benefits.triggers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.benefits.Command;
import edgruberman.bukkit.benefits.Contribution;

/** Immediately if player is currently connected, otherwise next time player joins */
public class Online extends Join {

    public Online(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @Override
    public void add(final Contribution contribution) {
        // trigger must be added first as dispatch verifies contribution is assigned before dispatching
        super.add(contribution);

        if (Bukkit.getServer().getPlayerExact(contribution.player) != null) {
            this.command.dispatch(contribution);
            return;
        }
    }

}
