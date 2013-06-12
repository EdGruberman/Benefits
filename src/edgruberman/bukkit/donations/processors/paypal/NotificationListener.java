package edgruberman.bukkit.donations.processors.paypal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import edgruberman.bukkit.donations.processors.PayPal;
import edgruberman.bukkit.donations.processors.paypal.InstantPaymentNotification.Variable;
import edgruberman.bukkit.donations.util.SynchronousPluginLogger;

/**
 * monitors for incoming asynchronous Instant Payment Notifications from
 * PayPal and queues them for synchronous processing
 */
public class NotificationListener implements HttpHandler, Runnable {

    private static final int BATCH = 5; // maximum count of IPNs to process each tick
    private static final int BUFFER = 1024; // length in bytes of read buffer
    private static final int MEDIAN = 1024; // length in bytes of typical IPN content
    private static final int LIMIT = 16384; // length in bytes of largest IPN request body before ignoring

    private final PayPal processor;
    private final Plugin plugin;
    private final Logger logger;

    private final HttpServer server;
    private final HttpWhitelist whitelist;
    private final HttpValidator validator;

    private final Queue<InstantPaymentNotification> pending = new ConcurrentLinkedQueue<InstantPaymentNotification>();
    private int taskId = -1;
    private final Object lock = new Object();
    private boolean stopping = false;

    /**
     * @param whitelist allowed remote host name
     * @throws UnknownHostException if whitelist has an invalid address
     * @throws MalformedURLException if validator is an invalid address
     * @throws IOException for any exception while attempting to create HTTP server
     */
    public NotificationListener(final PayPal processor, final Plugin plugin, final String validator, final InetSocketAddress bind, final List<String> whitelist)
            throws UnknownHostException, MalformedURLException, IOException {
        this.processor = processor;
        this.plugin = plugin;
        this.logger = new SynchronousPluginLogger(plugin).setPattern("[PayPal] {0}");

        this.whitelist = ( whitelist != null ? new HttpWhitelist(this.logger, whitelist) : null );
        this.validator = new HttpValidator(this.logger, validator);

        this.server = HttpServer.create(bind, -1);
        final HttpContext context = this.server.createContext("/", this);

        if (this.whitelist != null) context.getFilters().add(this.whitelist);
        context.getFilters().add(new HttpMethodAccepter(this.logger, "POST"));
        context.getFilters().add(new HttpBodyParser(this.logger, NotificationListener.BUFFER, NotificationListener.MEDIAN, NotificationListener.LIMIT));
        context.getFilters().add(this.validator);

        this.server.start();
    }

    public void stop() {
        // prevent further notifications from queuing
        this.server.stop(0);

        // cancel any scheduled task
        synchronized (this.lock) {
            Bukkit.getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
            this.stopping = true;
        }

        // flush notification queue
        this.run();
    }

    public InetSocketAddress getAddress() {
        return this.server.getAddress();
    }

    public URL getValidator() {
        return this.validator.getValidator();
    }

    public Set<InetAddress> getWhitelist() {
        return ( this.whitelist != null ? this.whitelist.getAddresses() : null );
    }

    /** asynchronous capture of incoming IPNs */
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        // confirm receipt
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);

        // construct IPN
        final String body = (String) exchange.getAttribute("body");
        InstantPaymentNotification ipn;
        try {
            ipn = new InstantPaymentNotification(body);
        } catch (final UnsupportedEncodingException e) {
            this.logger.log(Level.FINER, "IPN exception from {0}; {1}; {2}", new Object[] { exchange.getRemoteAddress().getAddress().getHostAddress(), e, body });
            return;
        }

        // debug logging
        if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.log(Level.FINEST, "IPN received from {0}; {1}; {2}={3}; {4}={5}"
                    , new Object[] { exchange.getRemoteAddress().getAddress().getHostAddress(), ipn.parseTransactionType()
                    , Variable.PAYER_EMAIL, ipn.getValue(Variable.PAYER_EMAIL)
                    , Variable.GROSS, ipn.getValue(Variable.GROSS)
                    , });
        }

        // append IPN to queue
        this.pending.add(ipn);

        // schedule to step back into main thread, if not already scheduled
        synchronized (this.lock) {
            if (this.taskId != -1) return;
            this.taskId = Bukkit.getScheduler().runTask(NotificationListener.this.plugin, this).getTaskId();
        }
    }

    /** synchronous batch processing of IPN queue */
    @Override
    public void run() {
        InstantPaymentNotification ipn;
        int count = 0;
        while ((ipn = this.pending.poll()) != null) {
            NotificationListener.this.processor.process(ipn);
            if (!this.stopping && ++count >= NotificationListener.BATCH) break;
        }

        // schedule to run again at next opportunity if notifications still pending
        synchronized (this.lock) {
            this.taskId = ( (this.pending.isEmpty() || this.stopping) ? -1 : Bukkit.getScheduler().runTask(NotificationListener.this.plugin, this).getTaskId() );
        }
    }

}
