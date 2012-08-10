package edgruberman.bukkit.donations.commands;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Main;

public final class Process implements CommandExecutor {

    private final Coordinator coordinator;

    public Process(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // usage: /<command> <Donator> <Amount>[ <When>]
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length < 1) {
            Main.courier.send(sender, "messages.requiresArgument", "<Donator>");
            return false;
        }

        if (args.length < 2) {
            Main.courier.send(sender, "messages.requiresArgument", "<Amount>");
            return false;
        }

        final String donator = args[0];
        final Double amount = Process.parseDouble(args[1]);
        if (amount == null) {
            Main.courier.send(sender, "messages.unknownArgument", "<Amount>", args[1]);
            return false;
        }

        final Date when = (args.length >= 3 ? Process.parseDate(args[2]) : new Date());
        if (when == null) {
            Main.courier.send(sender, "messages.unknownArgument", "<Amount>", args[2]);
            return false;
        }

        final Donation donation = new Donation(sender.getName(), donator, amount, when.getTime(), null);
        this.coordinator.process(donation);
        Main.courier.send(sender, "messages.process.success", donator, amount, Process.join(donation.packages, "messages.process.packages"));
        return true;
    }

    private static Double parseDouble(final String s) {
        try { return Double.parseDouble(s);
        } catch(final Exception e) { return null; }
    }

    private static Date parseDate(final String s) {
        try { return (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz")).parse(s);
        } catch (final ParseException e) { return null; }
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
