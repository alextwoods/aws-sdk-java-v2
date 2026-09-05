package software.amazon.awssdk.benchmark.e2e;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The catalogue of faults {@link MockDdbServer} can inject, shared with {@code ErrorBehaviorProbe} so
 * that the server and the probe cannot disagree about what a mode means.
 *
 * <p>Chosen to span the axes along which a bridged pipeline can plausibly differ from stock v2 — one
 * fault per axis rather than a long list of variations on the same axis:
 *
 * <ul>
 *   <li><b>modeled and retryable</b> ({@code throughput-exceeded}, {@code internal-error}): the error
 *       has to unmarshal into the right v2 exception type <em>and</em> be classified as retryable.
 *   <li><b>unmodeled but retryable</b> ({@code throttling}): DynamoDB does not model
 *       {@code ThrottlingException}, so v2 falls back to {@code DynamoDbException} and classifies from
 *       the error code alone. Tests the error-code path without the modeled-shape path.
 *   <li><b>retryable from status only</b> ({@code unavailable}): no parseable body at all, so the
 *       classification can only come from the HTTP status code.
 *   <li><b>{@code Retry-After} honoring</b> ({@code unavailable-retry-after}): recorded as MISSING in
 *       the ledger (1.5); this makes the difference observable rather than asserted.
 *   <li><b>modeled and <em>not</em> retryable</b> ({@code resource-not-found}): a bridge that
 *       over-retries is as wrong as one that under-retries, and this catches it.
 *   <li><b>modeled by the service but not by the operation</b> ({@code conditional-check-failed}):
 *       {@code ConditionalCheckFailedException} is a DynamoDB error but not one {@code GetItem}
 *       declares, so neither pipeline can map it from the operation's error list. This is the path
 *       where the two diverge most (ledger 1.3).
 *   <li><b>unparseable success</b> ({@code malformed-body}): a 200 whose body is truncated JSON, which
 *       exercises the deserializer's failure path rather than the error path.
 *   <li><b>empty error body</b> ({@code empty-500}): a 5xx with no body, where v2 synthesizes an
 *       exception from nothing.
 *   <li><b>torn response body</b> ({@code truncated-stream}): a 200 whose {@code Content-Length}
 *       promises more than the body delivers, so the client fails reading the socket rather than
 *       parsing the payload.
 *   <li><b>transport failure</b> ({@code connection-reset}): the socket is closed with no response at
 *       all. Because bodies are streamed, this is the <em>only</em> fault here that fails inside the
 *       transport, and so the only one that reaches the axis along which a bridged pipeline loses
 *       retries wholesale: smithy-java drives retries from inside {@code ClientPipeline.deserialize},
 *       which a failed send never reaches, so nothing here is ever retried where v2 retries every
 *       {@code IOException} (ledger 3.6). The single largest behavioral difference on the branch, and
 *       the catalogue did not cover it until it was added.
 * </ul>
 *
 * <p>Those last two look like one axis and are two. Keeping both is what established that they differ —
 * a truncated body fails after the transport has already returned the headers, so it never becomes a
 * {@code TransportException} and never touches 3.6.
 *
 * <p>Every fault response carries an {@code x-amzn-RequestId} header, because request-ID propagation onto
 * the thrown exception is itself a ledger item (1.4).
 */
public final class Faults {

    /** The request ID every fault response reports, so a probe can assert on an exact value. */
    public static final String REQUEST_ID = "FAULTREQID000000000000000";

    private static final String DDB_NS = "com.amazonaws.dynamodb.v20120810#";
    private static final String JSON = "application/x-amz-json-1.0";

    /** A canned HTTP response: status, content type, body, and any extra headers. */
    public static final class Fault {
        private final String mode;
        private final int status;
        private final String contentType;
        private final byte[] body;
        private final Map<String, String> headers;
        private final String description;
        private final int declaredLengthPadding;
        private final boolean abortConnection;

        private Fault(String mode, int status, String contentType, String body,
                      Map<String, String> headers, String description, int declaredLengthPadding,
                      boolean abortConnection) {
            this.mode = mode;
            this.status = status;
            this.contentType = contentType;
            this.body = body.getBytes(StandardCharsets.UTF_8);
            this.headers = headers;
            this.description = description;
            this.declaredLengthPadding = declaredLengthPadding;
            this.abortConnection = abortConnection;
        }

        public String mode() {
            return mode;
        }

        public int status() {
            return status;
        }

        public String contentType() {
            return contentType;
        }

        public byte[] body() {
            return body;
        }

        public Map<String, String> headers() {
            return headers;
        }

        public String description() {
            return description;
        }

        /**
         * Bytes to over-declare in {@code Content-Length} beyond what the body actually contains.
         *
         * <p>Zero for every fault that is a well-formed HTTP response. Non-zero produces a
         * <em>transport</em>-level failure rather than a protocol- or payload-level one: the client
         * reads to the declared length, the connection ends first, and the HTTP client raises a
         * premature-EOF {@code IOException} — the closest a canned response can get to a connection
         * reset without reaching into the servlet container's connection.
         *
         * <p>Only works because {@code MockDdbServer} commits the response before returning; see the
         * note there. Left uncommitted, the container turns the shortfall into a clean 500, which is
         * indistinguishable from {@code empty-500} and measures nothing.
         */
        public int declaredLengthPadding() {
            return declaredLengthPadding;
        }

        /**
         * Whether the server should close the socket without sending any response at all.
         *
         * <p>The distinction from {@link #declaredLengthPadding()} is not cosmetic, and it took a
         * measurement to find: bodies are <em>streamed</em>, so a client's HTTP call returns as soon as
         * the response headers arrive and the body is read afterwards. A truncated body therefore fails
         * <em>outside</em> the transport, arriving at the retry layer as whatever the codec threw. Only
         * a failure before the headers — this — fails inside the transport, and smithy-java's retry loop
         * sits below that: it runs inside {@code ClientPipeline.deserialize}, so a failed send is
         * rethrown without ever reaching code that could retry it (ledger 3.6). This is the only fault in
         * the catalogue that reaches the defect that entry describes.
         *
         * <p>Also the most realistic failure here: an idle pooled connection reaped by a load balancer
         * looks exactly like this to the client.
         */
        public boolean abortConnection() {
            return abortConnection;
        }
    }

    private static final Map<String, Fault> FAULTS = new LinkedHashMap<>();

    private static void add(String mode, int status, String contentType, String body,
                            Map<String, String> extraHeaders, String description) {
        add(mode, status, contentType, body, extraHeaders, description, 0);
    }

    private static void add(String mode, int status, String contentType, String body,
                            Map<String, String> extraHeaders, String description, int padding) {
        put(mode, status, contentType, body, extraHeaders, description, padding, false);
    }

    /** A fault with no response at all: the server closes the socket. */
    private static void addAbort(String mode, String description) {
        put(mode, 0, "", "", Map.of(), description, 0, true);
    }

    private static void put(String mode, int status, String contentType, String body,
                            Map<String, String> extraHeaders, String description, int padding,
                            boolean abort) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-amzn-RequestId", REQUEST_ID);
        headers.putAll(extraHeaders);
        FAULTS.put(mode, new Fault(mode, status, contentType, body, headers, description, padding, abort));
    }

    private static String errorBody(String shape, String message) {
        return "{\"__type\":\"" + shape + "\",\"message\":\"" + message + "\"}";
    }

    static {
        add("throughput-exceeded", 400, JSON,
            errorBody(DDB_NS + "ProvisionedThroughputExceededException", "injected fault"),
            Map.of(), "modeled, retryable, throttling");
        add("throttling", 400, JSON,
            errorBody("com.amazon.coral.availability#ThrottlingException", "injected fault"),
            Map.of(), "unmodeled, retryable, throttling");
        add("internal-error", 500, JSON,
            errorBody(DDB_NS + "InternalServerError", "injected fault"),
            Map.of(), "modeled, retryable, server-side");
        add("unavailable", 503, "text/plain", "Service Unavailable",
            Map.of(), "no parseable body; retryable from status alone");
        add("unavailable-retry-after", 503, "text/plain", "Service Unavailable",
            Map.of("Retry-After", "1"), "as above, plus a Retry-After hint (ledger 1.5)");
        add("resource-not-found", 400, JSON,
            errorBody(DDB_NS + "ResourceNotFoundException", "injected fault"),
            Map.of(), "modeled on GetItem, NOT retryable");
        add("conditional-check-failed", 400, JSON,
            errorBody(DDB_NS + "ConditionalCheckFailedException", "injected fault"),
            Map.of(), "modeled by DynamoDB but NOT by GetItem; the operation-unknown error path");
        add("malformed-body", 200, JSON, "{\"Item\":{\"pk\":{\"S\":\"trunc",
            Map.of(), "200 with truncated JSON; deserializer failure path");
        add("empty-500", 500, JSON, "",
            Map.of(), "5xx with an empty body");
        add("truncated-stream", 200, JSON, "{\"Item\":{\"pk\":{\"S\":\"trunc",
            Map.of(), "200 whose Content-Length over-declares the body; EOF while reading the body", 64);
        addAbort("connection-reset",
                 "socket closed with no response; the only fault that fails inside the transport");
    }

    private Faults() {
    }

    /** The fault for a mode, or null if the mode is unknown (including {@code none}). */
    public static Fault get(String mode) {
        return mode == null ? null : FAULTS.get(mode);
    }

    /** Every mode, in declaration order — which is also the order the probe reports them. */
    public static Iterable<String> modes() {
        return FAULTS.keySet();
    }
}
