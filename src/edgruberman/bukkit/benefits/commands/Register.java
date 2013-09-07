package edgruberman.bukkit.benefits.commands;

import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import edgruberman.bukkit.benefits.Coordinator;
import edgruberman.bukkit.benefits.Contribution;
import edgruberman.bukkit.benefits.Main;

public final class Register implements CommandExecutor {

    private final Coordinator coordinator;

    public Register(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // usage: '§f-> §7Usage: §b§l/<command> §3§oorigin §3[§oplayer§3]'
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length < 1) {
            Main.courier.send(sender, "requires-argument", "origin", 0);
            return false;
        }

        final String origin = args[0];
        final String player = ( args.length < 2 ? null : args[1] );

        final List<Contribution> unassigned = this.coordinator.unassigned(origin);
        Main.courier.send(sender, "register.unassigned.header", origin);
        for (final Contribution contribution : unassigned) Main.courier.send(sender, "register.unassigned.item", contribution.processor, contribution.id, contribution.currency, contribution.amount / 100D, contribution.contributed);
        Main.courier.send(sender, "register.unassigned.footer", unassigned.size());
        if (player == null) return true;

        final String previous = this.coordinator.putRegistration(origin.toLowerCase(Locale.ENGLISH), player.toLowerCase(Locale.ENGLISH));
        Main.courier.send(sender, "register.success", origin, player, previous);
        return true;
    }

}
