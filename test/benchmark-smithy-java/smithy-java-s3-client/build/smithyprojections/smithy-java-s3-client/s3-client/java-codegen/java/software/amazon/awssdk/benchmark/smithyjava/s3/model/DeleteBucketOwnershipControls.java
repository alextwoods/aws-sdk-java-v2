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
 * <p>Removes <code>OwnershipControls</code> for an Amazon S3 bucket. To use this operation, you must have the <code>
 * s3:PutBucketOwnershipControls</code> permission. For more information about Amazon S3 permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-with-s3-actions.html">Specifying
 * Permissions in a Policy</a>.
 *
 * <p>For information about Amazon S3 Object Ownership, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/about-object-ownership.html">Using Object Ownership</a>.
 *
 * <p>The following operations are related to <code>DeleteBucketOwnershipControls</code>:
 *
 * <ul>
 *   <li>
 *     <a>GetBucketOwnershipControls</a>
 *   </li>
 *   <li>
 *     <a>PutBucketOwnershipControls</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class DeleteBucketOwnershipControls implements ApiOperation<DeleteBucketOwnershipControlsInput, DeleteBucketOwnershipControlsOutput> {

    private static final DeleteBucketOwnershipControls $INSTANCE = new DeleteBucketOwnershipControls();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteBucketOwnershipControls"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("DELETE").code(204).uri(UriPattern.parse("/{Bucket}?ownershipControls")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteBucketOwnershipControls instance() {
        return $INSTANCE;
    }

    private DeleteBucketOwnershipControls() {}

    @Override
    public ShapeBuilder<DeleteBucketOwnershipControlsInput> inputBuilder() {
        return DeleteBucketOwnershipControlsInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteBucketOwnershipControlsOutput> outputBuilder() {
        return DeleteBucketOwnershipControlsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteBucketOwnershipControlsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteBucketOwnershipControlsOutput.$SCHEMA;
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
