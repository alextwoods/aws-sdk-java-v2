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
 * <p>Deletes tags from the general purpose bucket if attribute based access control (ABAC) is not enabled for the
 * bucket. When you <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/buckets-tagging-enable-abac.html">enable ABAC for a general purpose bucket</a>, you can no longer use this operation for that
 * bucket and must use <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_control_UntagResource.html">UntagResource</a> instead.
 *
 * <p>To use this operation, you must have permission to perform the <code>s3:PutBucketTagging</code> action. By
 * default, the bucket owner has this permission and can grant this permission to others.
 *
 * <p>The following operations are related to <code>DeleteBucketTagging</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketTagging.html">GetBucketTagging</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketTagging.html">PutBucketTagging</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To delete bucket tags</h3>
 *
 * <p>The following example deletes bucket tags.{@snippet :
 * var input = DeleteBucketTaggingInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.deleteBucketTagging(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class DeleteBucketTagging implements ApiOperation<DeleteBucketTaggingInput, DeleteBucketTaggingOutput> {

    private static final DeleteBucketTagging $INSTANCE = new DeleteBucketTagging();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteBucketTagging"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("DELETE").code(204).uri(UriPattern.parse("/{Bucket}?tagging")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteBucketTagging instance() {
        return $INSTANCE;
    }

    private DeleteBucketTagging() {}

    @Override
    public ShapeBuilder<DeleteBucketTaggingInput> inputBuilder() {
        return DeleteBucketTaggingInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteBucketTaggingOutput> outputBuilder() {
        return DeleteBucketTaggingOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteBucketTaggingInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteBucketTaggingOutput.$SCHEMA;
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
