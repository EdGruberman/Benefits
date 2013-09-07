package edgruberman.bukkit.benefits.processors.paypal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

class HttpBodyParser extends Filter {

    private final Logger logger;
    private final int buffer;
    private final int median;
    private final int limit;

    /**
     * @param buffer size in bytes of read buffer
     * @param median initial size in bytes of content container (smaller than median could mean lots of resizing, larger than median could mean wasted memory)
     * @param limit maximum size in bytes before request is entirely discarded
     */
    HttpBodyParser(final Logger logger, final int buffer, final int median, final int limit) {
        this.logger = logger;
        this.buffer = buffer;
        this.median = median;
        this.limit = limit;
    }

    @Override
    public String description() {
        return "Parse request body and close exchanges that exceed configured size limit";
    }

    @Override
    public void doFilter(final HttpExchange exchange, final Chain chain) throws IOException {
        final InputStreamReader br = new InputStreamReader(exchange.getRequestBody(), HttpBodyParser.parseCharset(exchange));
        final StringBuffer sb = new StringBuffer(this.median);
        final char[] cbuf = new char[this.buffer];
        int read;
        while ((read = br.read(cbuf)) != -1) {
            sb.append(cbuf, 0, read);
            if (sb.length() > this.limit) {
                exchange.close();
                this.logger.log(Level.FINER, "Request body exceeded limit: {0}; Remote: {1}", new Object[] { this.limit, exchange.getRemoteAddress() });
                return;
            }
        }
        exchange.setAttribute("body", sb.toString());

        chain.doFilter(exchange);
    }

    private static String parseCharset(final HttpExchange exchange) {
        final String charset = exchange.getRequestHeaders().getFirst("Content-Encoding");
        if (charset != null) return charset;

        final String[] contentType = exchange.getRequestHeaders().getFirst("Content-Type").split(";");
        for (String pair : contentType) {
            pair = pair.trim();
            if (pair.trim().toLowerCase().startsWith("charset="))
                return pair.substring("charset=".length());
        }

        return "UTF-8"; // default assumption - http://www.w3.org/TR/REC-xml/#charencoding
    }

}
