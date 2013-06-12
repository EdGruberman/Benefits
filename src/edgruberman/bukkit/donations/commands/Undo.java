package edgruberman.bukkit.donations.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.bukkit.command.CommandSender;

import edgruberman.bukkit.donations.Benefit;
import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Main;
import edgruberman.bukkit.donations.Package;
import edgruberman.bukkit.donations.util.JoinList;

public final class Undo extends Executor {

    private final Coordinator coordinator;

    public Undo(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // usage: /<command> <Donation> <Package>[ <Benefit>[ <Command>]]
    @Override
    protected boolean execute(final CommandSender sender, final org.bukkit.command.Command command, final String label, final List<String> args) {
        if (args.size() < 1) {
            Main.courier.send(sender, "requires-argument", "donation", false);
            return false;
        }

        final Donation donation = this.coordinator.getDonation(args.get(0));
        if (donation == null) {
            Main.courier.send(sender, "unknown-argument", "donation", false, args.get(0));
            return false;
        }

        if (args.size() < 2) {
            Main.courier.send(sender, "requires-argument", "package", false);
            return false;
        }

        final Package pkg = this.coordinator.packages.get(args.get(1).toLowerCase());
        if (pkg == null) {
            Main.courier.send(sender, "unknown-argument", "package", false, args.get(1));
            return false;
        }

        Benefit benefit = null;
        if (args.size() >= 3) {
            benefit = pkg.benefits.get(args.get(2).toLowerCase());
            if (benefit == null) {
                Main.courier.send(sender, "unknown-argument", "benefit", false, args.get(2));
                return false;
            }
        }

        Command cmd = null;
        if (args.size() >= 4) {
            cmd = benefit.commands.get(args.get(3).toLowerCase());
            if (cmd == null) {
                Main.courier.send(sender, "unknown-argument", "command", false, args.get(3));
                return false;
            }
        }

        final Collection<String> removed = new ArrayList<String>();
        if (cmd != null) {
            cmd.undo(donation);
            removed.add(cmd.getPath());

        } else if (benefit != null) {
            for (final Command c : benefit.commands.values()) {
                c.undo(donation);
                removed.add(c.getPath());
            }

        } else {
            for (final Benefit b : pkg.benefits.values())
                for (final Command c : b.commands.values()) {
                    c.undo(donation);
                    removed.add(c.getPath());
                }
        }

        this.coordinator.savePending();
        Main.courier.send(sender, "undo.success"
                , donation.player, donation.amount, new Date(donation.contributed)
                , new JoinList<String>(Main.courier.getSection("undo.commands"), removed));

        return true;

    }

}
