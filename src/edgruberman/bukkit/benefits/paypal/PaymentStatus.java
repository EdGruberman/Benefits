package edgruberman.bukkit.benefits.paypal;

import java.util.HashMap;
import java.util.Map;

public enum PaymentStatus {

    /**
     * A reversal has been canceled. For example, you won a dispute with the
     * customer, and the funds for the transaction that was reversed have
     * been returned to you.
     */
    CANCELED_REVERSAL("Canceled_Reversal")

    /**
     * The payment has been completed, and the funds have been added
     * successfully to your account balance.
     */
    , COMPLETED("Completed")

    /**
     * A German ELV payment is made using Express Checkout.
     */
    , CREATED("Created")

    /**
     * The payment was denied. This happens only if the payment was
     * previously pending because of one of the reasons listed for the
     * pending_reason variable or the Fraud_Management_Filters_x variable.
     */
    , DENIED("Denied")

    /**
     * This authorization has expired and cannot be captured.
     */
    , EXPIRED("Expired")

    /**
     * The payment has failed. This happens only if the payment was made from
     * your customer's bank account.
     */
    , FAILED("Failed")

    /**
     * The payment is pending. See pending_reason for more information.
     */
    , PENDING("Pending")

    /**
     * You refunded the payment.
     */
    , REFUNDED("Refunded")

    /**
     * A payment was reversed due to a chargeback or other type of reversal.
     * The funds have been removed from your account balance and returned to
     * the buyer. The reason for the reversal is specified in the ReasonCode
     * element.
     */
    , REVERSED("Reversed")

    /**
     * A payment has been accepted.
     */
    , PROCESSED("Processed")

    /**
     * This authorization has been voided.
     */
    , VOIDED("Voided")

    ;


    // -- static matcher --

    private static final Map<String, PaymentStatus> KNOWN = new HashMap<String, PaymentStatus>();

    static {
        for (final PaymentStatus e : PaymentStatus.values())
            PaymentStatus.KNOWN.put(e.getValue(), e);
    }

    public static PaymentStatus of(final String value) {
        return PaymentStatus.KNOWN.get(value);
    }


    // -- instance definition --

    private final String value;

    private PaymentStatus(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return InstantPaymentNotification.Variable.PAYMENT_STATUS.getKey() + "=" + this.value;
    }

}
