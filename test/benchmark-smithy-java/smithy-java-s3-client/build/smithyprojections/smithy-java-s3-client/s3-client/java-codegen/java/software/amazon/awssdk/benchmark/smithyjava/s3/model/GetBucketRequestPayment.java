package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Returns the request payment configuration of a bucket. To use this version of the operation, you must be the
 * bucket owner. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/RequesterPaysBuckets.html">Requester Pays Buckets</a>.
 *
 * <p>The following operations are related to <code>GetBucketRequestPayment</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">ListObjects</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To get bucket versioning configuration</h3>
 *
 * <p>The following example retrieves bucket versioning configuration.{@snippet :
 * var input = GetBucketRequestPaymentInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketRequestPayment(input);
 * result.equals(GetBucketRequestPaymentOutput.builder()
 *                   .payer(Payer.BUCKET_OWNER)
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketRequestPayment implements ApiOperation<GetBucketRequestPaymentInput, GetBucketRequestPaymentOutput> {

    private static final GetBucketRequestPayment $INSTANCE = new GetBucketRequestPayment();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketRequestPayment"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?requestPayment")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketRequestPayment instance() {
        return $INSTANCE;
    }

    private GetBucketRequestPayment() {}

    @Override
    public ShapeBuilder<GetBucketRequestPaymentInput> inputBuilder() {
        return GetBucketRequestPaymentInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketRequestPaymentOutput> outputBuilder() {
        return GetBucketRequestPaymentOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketRequestPaymentInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketRequestPaymentOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of();
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return SCHEMES;
    }

    @Override
    public Schema inputStreamMember() {
        return null;
    }

    @Override
    public Schema outputStreamMember() {
        return null;
    }

    @Override
    public Schema idempotencyTokenMember() {
        return null;
    }

    @Override
    public ApiService service() {
        return S3ApiService.instance();
    }
    }
