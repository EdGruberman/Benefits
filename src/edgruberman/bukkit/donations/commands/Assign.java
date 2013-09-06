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

public final class Assign extends Executor {

    private final Coordinator coordinator;

    public Assign(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // usage: /<command> <Donation> <Package>[ <Benefit>[ <Command>]]
    @Override
    protected boolean execute(final CommandSender sender, final org.bukkit.command.Command command, final String label, final List<String> args) {
        if (args.size() < 1) {
            Main.courier.send(sender, "requires-argument", "donation", 0);
            return false;
        }

        final Donation donation = this.coordinator.getDonation(args.get(0));
        if (donation == null) {
            Main.courier.send(sender, "unknown-argument", "donation", 0, args.get(0));
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

        final Collection<String> assigned = new ArrayList<String>();
        if (cmd != null) {
            cmd.assign(donation);
            assigned.add(cmd.getPath());

        } else if (benefit != null) {
            for (final Command c : benefit.assign(donation)) {
                assigned.add(c.getPath());
            }

        } else {
            for (final Command c : pkg.assign(donation)) {
                assigned.add(c.getPath());
            }
        }

        this.coordinator.savePending();
        Main.courier.send(sender, "assign.success"
                , donation.player, donation.currency, donation.amount / 100D, new Date(donation.contributed)
                , new JoinList<String>(Main.courier.getSection("assign.commands"), assigned));

        return true;

    }

}
