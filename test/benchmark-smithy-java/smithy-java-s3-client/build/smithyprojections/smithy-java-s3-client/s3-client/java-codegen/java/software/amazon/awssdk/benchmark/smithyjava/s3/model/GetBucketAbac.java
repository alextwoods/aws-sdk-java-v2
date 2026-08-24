package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Returns the attribute-based access control (ABAC) property of the general purpose bucket. If ABAC is enabled on your
 * bucket, you can use tags on the bucket for access control. For more information, see <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/buckets-tagging-enable-abac.html">Enabling ABAC in
 * general purpose buckets</a>.
 */
@SmithyGenerated
public final class GetBucketAbac implements ApiOperation<GetBucketAbacInput, GetBucketAbacOutput> {

    private static final GetBucketAbac $INSTANCE = new GetBucketAbac();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketAbac"),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?abac")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketAbac instance() {
        return $INSTANCE;
    }

    private GetBucketAbac() {}

    @Override
    public ShapeBuilder<GetBucketAbacInput> inputBuilder() {
        return GetBucketAbacInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketAbacOutput> outputBuilder() {
        return GetBucketAbacOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketAbacInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketAbacOutput.$SCHEMA;
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
