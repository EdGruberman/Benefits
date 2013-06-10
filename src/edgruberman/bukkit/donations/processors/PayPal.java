package edgruberman.bukkit.donations.processors;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Donation;
import edgruberman.bukkit.donations.Processor;
import edgruberman.bukkit.donations.processors.paypal.InstantPaymentNotification;
import edgruberman.bukkit.donations.processors.paypal.InstantPaymentNotification.Variable;
import edgruberman.bukkit.donations.processors.paypal.NotificationListener;
import edgruberman.bukkit.donations.processors.paypal.PaymentStatus;
import edgruberman.bukkit.donations.processors.paypal.TransactionType;

public class PayPal extends Processor {

    private final String email;
    private final String currency;
    private final NotificationListener listener;

    public PayPal(final Coordinator coordinator, final ConfigurationSection config) {
        super(coordinator, config);
        this.email = config.getString("email").toLowerCase();
        this.currency = config.getString("currency").toLowerCase();

        // parse listen binding
        final String ip = config.getString("listen-ip");
        InetAddress address;
        try {
            address = ( ip != null ? InetAddress.getByName(ip) : null );
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Invalid bind IP address: " + ip, e);
        }
        final int port = config.getInt("listen-port");
        final InetSocketAddress bind = new InetSocketAddress(address, port);

        // start listener
        try {
            this.listener = new NotificationListener(this, this.coordinator.plugin, config.getString("validator"), bind, config.getStringList("whitelist"));
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to start IPN listener bound to " + bind, e);
        }

        this.coordinator.plugin.getLogger().log(Level.CONFIG, "[PayPal] Listening for IPNs from {0,choice,0#any remote|1#{1}} at http://{2}:{3,number,#}/"
                , new Object[] { ( this.listener.getWhitelist() == null ? 0 : 1 ), this.listener.getWhitelist()
                , this.listener.getAddress().getAddress().getHostAddress(), this.listener.getAddress().getPort() });
    }

    @Override
    public void stop() {
        this.listener.stop();
    }

    public void process(final InstantPaymentNotification ipn) {
        long gross;
        Date contributed;
        try {
            this.inspect(ipn);
            gross = ipn.parseGross();
            contributed = ipn.parsePaymentDate();

        } catch (final Exception e) {
            this.coordinator.plugin.getLogger().log(Level.WARNING, "[PayPal] Payment processing aborted; {0}; IPN: {1}", new Object[] { e, ipn });
            return;
        }

        this.process(ipn.getValue(Variable.TRANSACTION_ID), ipn.getValue(Variable.PAYER_EMAIL), ipn.getValue(Variable.CUSTOM), gross, contributed.getTime());
    }

    private void inspect(final InstantPaymentNotification ipn) {
        final TransactionType type = ipn.parseTransactionType();
        if (TransactionType.WEB_ACCEPT != type && TransactionType.SUBSCRIPTION_PAYMENT != type)
            throw new IllegalStateException(MessageFormat.format("Unsupported transaction ({0})", ipn.getValue(Variable.TRANSACTION_TYPE)));

        final PaymentStatus status = ipn.parsePaymentStatus();
        if (PaymentStatus.COMPLETED != status)
            throw new IllegalStateException(MessageFormat.format("Incomplete payment ({0})", ipn.getValue(Variable.PAYMENT_STATUS)));

        final String id = ipn.getValue(Variable.TRANSACTION_ID);
        if (this.coordinator.getDonation(Donation.createKey(this.getId(), id)) != null)
            throw new IllegalStateException(MessageFormat.format("Duplicate transaction notification ({0})", id));

        final String receiver = ipn.getValue(Variable.RECEIVER_EMAIL);
        if (!this.email.equals(receiver))
            throw new IllegalStateException(MessageFormat.format("Mismatched receiver e-mail ({0})", receiver));

        final String currency = ipn.getValue(Variable.CURRENCY).toLowerCase();
        if (!this.currency.equals(currency))
            throw new IllegalStateException(MessageFormat.format("Mismatched currency ({0})", currency));
    }

}
