package edgruberman.bukkit.donations.commands;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Main;
import edgruberman.bukkit.donations.processors.SlashCommand;
import edgruberman.bukkit.donations.util.JoinList;

public final class Process implements CommandExecutor {

    SlashCommand processor;

    public Process(final Coordinator coordinator) {
        this.processor = new SlashCommand(coordinator, null);
    }

    // usage: /<command> <Donator> <Amount>[ <When>]
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length < 1) {
            Main.courier.send(sender, "requires-argument", "donator", 0);
            return false;
        }

        if (args.length < 2) {
            Main.courier.send(sender, "requires-argument", "amount", 0);
            return false;
        }

        final String donator = args[0];
        Long amount = Process.parseLong(args[1]);
        if (amount == null) {
            Main.courier.send(sender, "unknown-argument", "amount", false, args[1]);
            return false;
        }
        amount = amount * 100;

        final Date when = (args.length >= 3 ? Process.parseDate(args[2]) : new Date());
        if (when == null) {
            Main.courier.send(sender, "unknown-argument", "amount", false, args[2]);
            return false;
        }

        final Donation donation = this.processor.process(sender.getName(), donator, amount, when.getTime());
        final List<String> packages = new JoinList<String>(Main.courier.getSection("process.packages"), donation.packages);
        Main.courier.send(sender, "process.success", donator, donation.currency, amount / 100D, packages);
        return true;
    }

    private static Long parseLong(final String s) {
        try { return Long.parseLong(s);
        } catch(final Exception e) { return null; }
    }

    private static Date parseDate(final String s) {
        try { return (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz")).parse(s);
        } catch (final ParseException e) { return null; }
    }

}
