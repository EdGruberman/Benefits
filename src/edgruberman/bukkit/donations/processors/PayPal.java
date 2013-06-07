package edgruberman.bukkit.donations.processors;

import edgruberman.bukkit.donations.Coordinator;
import edgruberman.bukkit.donations.Processor;

// https://github.com/paypal/ipn-code-samples/blob/master/IPN_ASP_NET_VBS.txt
// http://docs.oracle.com/javase/6/docs/jre/api/net/httpserver/spec/index.html?overview-summary.html
// https://developer.paypal.com/webapps/developer/docs/classic/ipn/integration-guide/IPNIntro/
public class PayPal extends Processor {

    protected PayPal(final Coordinator coordinator) {
        super(coordinator);
    }

}
