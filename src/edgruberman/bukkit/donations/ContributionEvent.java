package edgruberman.bukkit.donations;

import java.util.Date;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ContributionEvent extends Event {

    private final OfflinePlayer contributor;
    private final Date contributed;
    private final String currency;
    private final long amount;

    ContributionEvent (final OfflinePlayer contributor, final Date contributed, final String currency, final long amount) {
        this.contributor = contributor;
        this.contributed = contributed;
        this.currency = currency;
        this.amount = amount;
    }

    public OfflinePlayer getContributor() {
        return this.contributor;
    }

    public Date getContributed() {
        return this.contributed;
    }

    public String getCurrency() {
        return this.currency;
    }

    public long getAmount() {
        return this.amount;
    }

    // ---- event handlers ----

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return ContributionEvent.handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return ContributionEvent.handlers;
    }

}
