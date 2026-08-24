package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Places an Object Retention configuration on an object. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lock.html">Locking Objects</a>. Users or
 * accounts require the <code>s3:PutObjectRetention</code> permission in order to place an Object Retention
 * configuration on objects. Bypassing a Governance Retention configuration requires the <code>
 * s3:BypassGovernanceRetention</code> permission.
 *
 * <p>This functionality is not supported for Amazon S3 on Outposts.
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class PutObjectRetention implements ApiOperation<PutObjectRetentionInput, PutObjectRetentionOutput> {

    private static final PutObjectRetention $INSTANCE = new PutObjectRetention();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutObjectRetention"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .withMember("requestChecksumRequired", true)
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?retention")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutObjectRetention instance() {
        return $INSTANCE;
    }

    private PutObjectRetention() {}

    @Override
    public ShapeBuilder<PutObjectRetentionInput> inputBuilder() {
        return PutObjectRetentionInput.builder();
    }

    @Override
    public ShapeBuilder<PutObjectRetentionOutput> outputBuilder() {
        return PutObjectRetentionOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutObjectRetentionInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutObjectRetentionOutput.$SCHEMA;
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
