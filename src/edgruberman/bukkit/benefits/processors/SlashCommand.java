package edgruberman.bukkit.benefits.processors;

import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.benefits.Coordinator;
import edgruberman.bukkit.benefits.Contribution;
import edgruberman.bukkit.benefits.Processor;

public class SlashCommand extends Processor {

    public SlashCommand(final Coordinator coordinator, final ConfigurationSection config) {
        super(coordinator, config);
    }

    public Contribution process(final String origin, final String player, final long amount, final long contributed) {
        return this.process(UUID.randomUUID().toString(), origin, player, this.coordinator.currency, amount, contributed);
    }

}
