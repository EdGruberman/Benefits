package edgruberman.bukkit.benefits.triggers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerRespawnEvent;

import edgruberman.bukkit.benefits.Command;

public class Respawn extends PlayerEvent {

    public Respawn(final Command command, final ConfigurationSection definition) {
        super(command, definition);
    }

    @EventHandler
    public void onEvent(final PlayerRespawnEvent respawn) {
        this.apply(respawn.getPlayer());
    }

}