package edgruberman.bukkit.donations.processors.paypal;

import java.util.HashMap;
import java.util.Map;

public enum TransactionType {

    /**
     * A dispute has been resolved and closed
     */
    ADJUSTMENT("adjustment")

    /**
     * Payment received for multiple items; source is Express Checkout
     * or the PayPal Shopping Cart.
     */
    , CART("cart")

    /**
     * Payment received for a single item; source is Express Checkout
     */
    , EXPRESS_CHECKOUT("express_checkout")

    /**
     * Payment sent using MassPay
     */
    , MASSPAY("masspay")

    /**
     * Billing agreement cancelled
     */
    , MASSPAY_CANCEL("mp_cancel")

    /**
     * Created a billing agreement
     */
    , MASSPAY_SIGNUP("mp_signup")

    /**
     * Monthly subscription paid for PayPal Payments Pro
     */
    , MERCHANT_PAYMENT("merch_pmt")

    /**
     * A new dispute was filed
     */
    , NEW_CASE("new_case")

    /**
     * A payout related to a global shipping transaction was completed.
     */
    , PAYOUT("payout")

    /**
     * Recurring payment received
     */
    , RECURRING_PAYMENT("recurring_payment")

    /**
     * Recurring payment expired
     */
    , RECURRING_PAYMENT_EXPIRED("recurring_payment_expired")

    /**
     * Recurring payment profile created
     */
    , RECURRING_PAYMENT_PROFILE_CREATED("recurring_payment_profile_created")

    /**
     * Recurring payment skipped; it will be retried
     * up to a total of 3 times, 5 days apart
     */
    , RECURRING_PAYMENT_SKIPPED("recurring_payment_skipped")

    /**
     * Payment received; source is the Send Money tab on the PayPal website
     */
    , SEND_MONEY("send_money")

    /**
     * Subscription canceled
     */
    , SUBSCRIPTION_CANCEL("subscr_cancel")

    /**
     * Subscription expired
     */
    , SUBSCRIPTION_EXPIRED("subscr_eot")

    /**
     * Subscription payment failed
     */
    , SUBSCRIPTION_FAILED("subscr_failed")

    /**
     * Subscription modified
     */
    , SUBSCRIPTION_MODIFY("subscr_modify")

    /**
     * Subscription payment received
     */
    , SUBSCRIPTION_PAYMENT("subscr_payment")

    /**
     * Subscription started
     */
    , SUBSCRIPTION_SIGNUP("subscr_signup")

    /**
     * Payment received; source is Virtual Terminal
     */
    , VIRTUAL_TERMINAL("virtual_terminal")

    /**
     * Payment received; source is a Buy Now, Donation
     * , or Auction Smart Logos button
     */
    , WEB_ACCEPT("web_accept")

    ;



    // -- static matcher --

    private static final Map<String, TransactionType> KNOWN = new HashMap<String, TransactionType>();

    static {
        for (final TransactionType e : TransactionType.values())
            TransactionType.KNOWN.put(e.getValue(), e);
    }

    public static TransactionType of(final String value) {
        return TransactionType.KNOWN.get(value);
    }


    // -- instance definition --

    private final String value;

    private TransactionType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return InstantPaymentNotification.Variable.TRANSACTION_TYPE.getKey() + "=" + this.value;
    }

}
