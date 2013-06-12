package edgruberman.bukkit.donations.processors;

import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Processor;

public class SlashCommand extends Processor {

    public SlashCommand(final Coordinator coordinator, final ConfigurationSection config) {
        super(coordinator, config);
    }

    public Donation process(final String origin, final String player, final long amount, final long contributed) {
        return this.process(UUID.randomUUID().toString(), origin, player, this.coordinator.currency, amount, contributed);
    }

}
