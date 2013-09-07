package edgruberman.bukkit.benefits.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.bukkit.command.CommandSender;

import edgruberman.bukkit.benefits.Benefit;
import edgruberman.bukkit.benefits.Command;
import edgruberman.bukkit.benefits.Contribution;
import edgruberman.bukkit.benefits.Coordinator;
import edgruberman.bukkit.benefits.Main;
import edgruberman.bukkit.benefits.Package;
import edgruberman.bukkit.benefits.util.JoinList;

public final class Undo extends Executor {

    private final Coordinator coordinator;

    public Undo(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // usage: /<command> <Contribution> <Package>[ <Benefit>[ <Command>]]
    @Override
    protected boolean execute(final CommandSender sender, final org.bukkit.command.Command command, final String label, final List<String> args) {
        if (args.size() < 1) {
            Main.courier.send(sender, "requires-argument", "contribution", 0);
            return false;
        }

        final Contribution contribution = this.coordinator.getContribution(args.get(0));
        if (contribution == null) {
            Main.courier.send(sender, "unknown-argument", "contribution", 0, args.get(0));
            return false;
        }

        if (args.size() < 2) {
            Main.courier.send(sender, "requires-argument", "package", 0);
            return false;
        }

        final Package pkg = this.coordinator.packages.get(args.get(1).toLowerCase());
        if (pkg == null) {
            Main.courier.send(sender, "unknown-argument", "package", 0, args.get(1));
            return false;
        }

        Benefit benefit = null;
        if (args.size() >= 3) {
            benefit = pkg.benefits.get(args.get(2).toLowerCase());
            if (benefit == null) {
                Main.courier.send(sender, "unknown-argument", "benefit", 0, args.get(2));
                return false;
            }
        }

        Command cmd = null;
        if (args.size() >= 4) {
            cmd = benefit.commands.get(args.get(3).toLowerCase());
            if (cmd == null) {
                Main.courier.send(sender, "unknown-argument", "command", 0, args.get(3));
                return false;
            }
        }

        final Collection<String> removed = new ArrayList<String>();
        if (cmd != null) {
            cmd.undo(contribution);
            removed.add(cmd.getPath());

        } else if (benefit != null) {
            for (final Command c : benefit.commands.values()) {
                c.undo(contribution);
                removed.add(c.getPath());
            }

        } else {
            for (final Benefit b : pkg.benefits.values())
                for (final Command c : b.commands.values()) {
                    c.undo(contribution);
                    removed.add(c.getPath());
                }
        }

        this.coordinator.savePending();
        Main.courier.send(sender, "undo.success"
                , contribution.player, contribution.currency, contribution.amount / 100D, new Date(contribution.contributed)
                , new JoinList<String>(Main.courier.getSection("undo.commands"), removed));

        return true;

    }

}
