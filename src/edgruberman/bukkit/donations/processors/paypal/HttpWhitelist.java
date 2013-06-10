package edgruberman.bukkit.donations.processors.paypal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

class HttpWhitelist extends Filter {

    private final Logger logger;
    private final Set<InetAddress> addresses = new HashSet<InetAddress>();

    HttpWhitelist(final Logger logger, final List<String> hosts) throws UnknownHostException {
        this.logger = logger;
        for (final String host : hosts) this.addresses.addAll(Arrays.asList(InetAddress.getAllByName(host)));
    }

    public Set<InetAddress> getAddresses() {
        return Collections.unmodifiableSet(this.addresses);
    }

    @Override
    public String description() {
        return "Closes exchange when remote is not designated as acceptable";
    }

    @Override
    public void doFilter(final HttpExchange exchange, final Chain chain) throws IOException {
        final InetAddress remote = exchange.getRemoteAddress().getAddress();
        if (!this.addresses.contains(remote)) {
            exchange.close();
            this.logger.log(Level.FINER, "Whitelist exclusion: {0}; Remote: {1}", new Object[] { remote, exchange.getRemoteAddress() });
            return;
        }

        chain.doFilter(exchange);
    }

}
