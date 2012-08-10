package edgruberman.bukkit.donations.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import edgruberman.bukkit.donations.Benefit;
import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Main;
import edgruberman.bukkit.donations.Package;

public final class Benefits extends Executor {

    private final Coordinator coordinator;

    public Benefits(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // usage: /<command>[ <Package>]
    @Override
    protected boolean execute(final CommandSender sender, final Command command, final String label, final List<String> args) {
        if (args.size() == 0) {
            if (this.coordinator.packages.size() == 0) {
                Main.courier.send(sender, "messages.benefits.none");
                return true;
            }

            // Donor: $5.00+ USD [30 day limit] One extra life, priority, channels, and colors
            // Spawn City: $10.00+ USD - Spawn anything, any time
            // To list benefits: /benefits "<Package>" (Example: /benefits Donor)
            for (final Package pkg : this.coordinator.packages.values()) {
                final String limit = (pkg.limit != null ? Main.courier.format("messages.benefits.+limit", pkg.limit): "");
                Main.courier.send(sender, "messages.benefits.package", pkg.name, pkg.description, pkg.minimum, limit);
            }
            Main.courier.send(sender, "messages.benefits.instruction");

            return true;
        }

        final Package pkg = this.coordinator.packages.get(args.get(0).toLowerCase());
        if (pkg == null || pkg.benefits.size() == 0) {
            Main.courier.send(sender, "messages.benefits.none");
            return true;
        }

        // Donor: Confirmation = Notification regarding a received donation
        // Donor: Resurrection = Extra life after death
        for (final Benefit benefit : pkg.benefits.values())
            Main.courier.send(sender, "messages.benefits.benefit", pkg.name, benefit.name, benefit.description);

        return true;
    }

}
