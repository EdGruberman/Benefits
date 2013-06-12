package edgruberman.bukkit.donations.commands;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Main;
import edgruberman.bukkit.donations.util.JoinList;

public final class History implements CommandExecutor {

    private final Coordinator coordinator;
    private final int pageSize = 4;

    public History(final Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    // 2008 Aug 01 06:11PM (1132d) = $0.99 USD
    // 2009 Aug 07 09:12PM (751d) = $0.99 USD
    // 2010 Aug 22 11:14PM (186d) = $0.99 USD
    // 2012 Jun 01 06:30AM (2d) = $15.00 USD [Donor]
    // First 1132 days | Last 2 days | Page 3 of 3
    // usage: /<command>[ <Page>][ <Player>]
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        final Integer page =  History.parsePage(args);

        if (args.length < 2 && !(sender instanceof Player)) {
            Main.courier.send(sender, "requires-argument", "page player", false);
            return false;
        }

        final String target = (args.length >= 2 ? Bukkit.getOfflinePlayer(args[1]).getName() : sender.getName());
        final List<Donation> history = this.coordinator.history(target);
        if (history.size() == 0) {
            Main.courier.send(sender, "history.none", target);
            return true;
        }

        final int total = (int) Math.ceil(history.size() / (double) this.pageSize);
        final int first = Math.min((page - 1) * this.pageSize, history.size() - 1);
        final int last = Math.min(first + this.pageSize, history.size());
        final long now = System.currentTimeMillis();
        for (final Donation donation : history.subList(first, last)) {
            final List<String> packages = new JoinList<String>(Main.courier.getSection("history.packages"), donation.packages);
            final long days = TimeUnit.MILLISECONDS.toDays(now - donation.contributed);
            System.out.println(donation.amount);
            Main.courier.send(sender, "history.donation", new Date(donation.contributed), days, donation.amount, packages);
        }

        final long oldest = TimeUnit.MILLISECONDS.toDays(now - history.get(0).contributed);
        final long newest = TimeUnit.MILLISECONDS.toDays(now - history.get(history.size() - 1).contributed);
        double sum = 0; for (final Donation donation : history) sum += donation.amount;
        Main.courier.send(sender, "history.summary", target, oldest, newest, page, total, history.size(), sum);
        return true;
    }

    private static int parsePage(final String[] args) {
        if (args.length == 0) return 1;

        try { return Math.max(Integer.parseInt(args[0]), 1); }
        catch(final Exception e) { return 1; }
    }

}
