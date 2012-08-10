package edgruberman.bukkit.donations.commands;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import org.bukkit.command.CommandSender;

import edgruberman.bukkit.donations.Benefit;
import edgruberman.bukkit.donations.Command;
import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Main;
import edgruberman.bukkit.donations.Package;
import edgruberman.bukkit.donations.messaging.messages.TimestampedMessage;

public final class Remove extends Executor {

    private final Coordinator coordinator;

    public Remove(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // usage: /<command> <Donation> <Package>[ <Benefit>[ <Command>]]
    @Override
    protected boolean execute(final CommandSender sender, final org.bukkit.command.Command command, final String label, final List<String> args) {
        if (args.size() < 1) {
            Main.courier.send(sender, "messages.requiresArgument", "<Donation>");
            return false;
        }

        final Donation donation = this.coordinator.processed.get(args.get(0).toLowerCase());
        if (donation == null) {
            Main.courier.send(sender, "messages.unknownArgument", "<Donation>", args.get(0));
            return false;
        }

        if (args.size() < 2) {
            Main.courier.send(sender, "messages.requiresArgument", "<Package>");
            return false;
        }

        final Package pkg = this.coordinator.packages.get(args.get(1).toLowerCase());
        if (pkg == null) {
            Main.courier.send(sender, "messages.unknownArgument", "<Package>", args.get(1));
            return false;
        }

        Benefit benefit = null;
        if (args.size() >= 3) {
            benefit = pkg.benefits.get(args.get(2).toLowerCase());
            if (benefit == null) {
                Main.courier.send(sender, "messages.unknownArgument", "<Benefit>", args.get(2));
                return false;
            }
        }

        Command cmd = null;
        if (args.size() >= 4) {
            cmd = benefit.commands.get(args.get(3).toLowerCase());
            if (cmd == null) {
                Main.courier.send(sender, "messages.unknownArgument", "<Command>", args.get(3));
                return false;
            }
        }

        final Calendar contributed = new GregorianCalendar();
        contributed.setTimeInMillis(donation.contributed);
        contributed.setTimeZone(TimestampedMessage.getTimeZone(sender));

        final Collection<String> removed = new ArrayList<String>();

        if (cmd != null) {
            cmd.remove(donation);
            removed.add(cmd.getPath());

        } else if (benefit != null) {
            for (final Command c : benefit.commands.values()) {
                c.remove(donation);
                removed.add(c.getPath());
            }

        } else {
            for (final Benefit b : pkg.benefits.values())
                for (final Command c : b.commands.values()) {
                    c.remove(donation);
                    removed.add(c.getPath());
                }
        }

        this.coordinator.savePending();
        Main.courier.send(sender, "messages.remove.success", donation.player, donation.amount, contributed, Remove.join(removed, "messages.remove.commands"));
        return true;

    }

    private static String join(final Collection<? extends String> col, final String path) {
        if (col == null || col.isEmpty()) return "";

        final String format = Main.courier.format(path + ".+item");
        final String delim = Main.courier.format(path + ".+delim");

        final StringBuilder sb = new StringBuilder();
        for (final String s : col) {
            if (sb.length() > 0) sb.append(delim);
            sb.append(String.format(format, s));
        }

        return sb.toString();
    }

}
