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
 * <p>Returns the versioning state of a bucket.
 *
 * <p>To retrieve the versioning state of a bucket, you must be the bucket owner.
 *
 * <p>This implementation also returns the MFA Delete status of the versioning state. If the MFA Delete status is <code>
 * enabled</code>, the bucket owner must use an authentication device to change the versioning state of the bucket.
 *
 * <p>The following operations are related to <code>GetBucketVersioning</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html">DeleteObject</a>
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
 * var input = GetBucketVersioningInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketVersioning(input);
 * result.equals(GetBucketVersioningOutput.builder()
 *                   .status(BucketVersioningStatus.ENABLED).mfaDelete(MFADeleteStatus.DISABLED)
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketVersioning implements ApiOperation<GetBucketVersioningInput, GetBucketVersioningOutput> {

    private static final GetBucketVersioning $INSTANCE = new GetBucketVersioning();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketVersioning"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?versioning")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketVersioning instance() {
        return $INSTANCE;
    }

    private GetBucketVersioning() {}

    @Override
    public ShapeBuilder<GetBucketVersioningInput> inputBuilder() {
        return GetBucketVersioningInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketVersioningOutput> outputBuilder() {
        return GetBucketVersioningOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketVersioningInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketVersioningOutput.$SCHEMA;
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
