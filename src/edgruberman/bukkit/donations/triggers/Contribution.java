package edgruberman.bukkit.donations.triggers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;

import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.ContributionEvent;

public class Contribution extends PlayerEvent {

    public Contribution(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @EventHandler
    public void onEvent(final ContributionEvent contribution) {
        this.apply(contribution.getContributor());
    }

}