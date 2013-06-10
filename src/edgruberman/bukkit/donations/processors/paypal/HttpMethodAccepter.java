package edgruberman.bukkit.donations.processors.paypal;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

class HttpMethodAccepter extends Filter {

    private final Logger logger;
    private final String accepted;

    HttpMethodAccepter(final Logger logger, final String accepted) {
        this.logger = logger;
        this.accepted = accepted.toUpperCase(Locale.ENGLISH);
    }

    @Override
    public String description() {
        return "Closes exchanges that are not the accepted method";
    }

    @Override
    public void doFilter(final HttpExchange exchange, final Chain chain) throws IOException {
        if (!exchange.getRequestMethod().toUpperCase(Locale.ENGLISH).equals(this.accepted)) {
            exchange.close();
            this.logger.log(Level.FINER, "Method excluded: {0}; Remote: {1}", new Object[] { exchange.getRequestMethod(), exchange.getRemoteAddress() });
            return;
        }

        chain.doFilter(exchange);
    }

}
