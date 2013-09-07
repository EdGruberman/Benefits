package edgruberman.bukkit.benefits.processors.paypal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.CharBuffer;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

class HttpValidator extends Filter {

    private static final String COMMAND = "cmd=_notify-validate&{0}"; // validation command request body, 0 = notification request body
    private static final String VALID = "VERIFIED"; // response body from PayPal validator that confirms transaction is valid
    private static final int LIMIT = 16; // size in bytes that is large enough to hold either VERIFIED or INVALID in UTF-8 encoding

    private final Logger logger;
    private final URL validator;

    /** @param validator URL to PayPal IPN verification API */
    HttpValidator(final Logger logger, final String validator) throws MalformedURLException {
        this.logger = logger;
        this.validator = new URL(validator);
    }

    public URL getValidator() {
        return this.validator;
    }

    @Override
    public String description() {
        return "Closes exchange when IPN is not validated by PayPal";
    }

    @Override
    public void doFilter(final HttpExchange exchange, final Chain chain) throws IOException {
        final String body = (String) exchange.getAttribute("body");

        final String content = MessageFormat.format(HttpValidator.COMMAND, body);
        String result = null;
        try {
            result = HttpValidator.post(this.validator, content, HttpValidator.LIMIT);
        } catch (final Exception e) {
            this.logger.log(Level.FINER, "Exception while attempting to validate IPN: {0}; Remote: {1}", new Object[] { e, exchange.getRemoteAddress() });
        }

        if (!HttpValidator.VALID.equals(result)) {
            exchange.close();
            this.logger.log(Level.FINER, "IPN not verified: {0}; Remote: {1}", new Object[] { result, exchange.getRemoteAddress() });
            return;
        }

        chain.doFilter(exchange);
    }

    /**
     * @param destination URL to send POST data to
     * @param content POST data
     * @param limit maximum size in bytes of response to return, -1 for no limit
     * @return response body
     */
    private static String post(final URL remote, final String content, final int limit) throws ProtocolException, IOException {
        final byte[] bytes = content.getBytes("UTF-8");

        CharBuffer result = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) remote.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            final DataOutputStream os = new DataOutputStream(connection.getOutputStream());
            os.write(bytes);
            os.flush();
            os.close();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IllegalStateException("HTTP Response Code: " + connection.getResponseCode() + " " + connection.getResponseMessage());

            final Reader response = new InputStreamReader(connection.getInputStream(), HttpValidator.parseCharset(connection));
            result = CharBuffer.allocate(limit);
            response.read(result);

        } finally {
            if(connection != null) connection.disconnect();
        }

        return result.flip().toString();
    }

    private static String parseCharset(final HttpURLConnection connection) {
        final String charset = connection.getContentEncoding();
        if (charset != null) return charset;

        final String contentType = connection.getContentType();
        final String[] pairs = contentType.split(";");

        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.toLowerCase().startsWith("charset="))
                return pair.substring("charset=".length());
        }

        return "UTF-8"; // default assumption - http://www.w3.org/TR/REC-xml/#charencoding
    }

}
