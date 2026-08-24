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
 * This operation is not supported for directory buckets.
 *
 * <p>Removes the entire tag set from the specified object. For more information about managing object tags, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-tagging.html">
 * Object Tagging</a>.
 *
 * <p>To use this operation, you must have permission to perform the <code>s3:DeleteObjectTagging</code> action.
 *
 * <p>To delete tags of a specific object version, add the <code>versionId</code> query parameter in the request. You
 * will need permission for the <code>s3:DeleteObjectVersionTagging</code> action.
 *
 * <p>The following operations are related to <code>DeleteObjectTagging</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObjectTagging.html">PutObjectTagging</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectTagging.html">GetObjectTagging</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To remove tag set from an object</h3>
 *
 * <p>The following example removes tag set associated with the specified object. If the bucket is versioning enabled, the operation removes tag set from the latest object version.{@snippet :
 * var input = DeleteObjectTaggingInput.builder()
 *                 .bucket("examplebucket").key("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.deleteObjectTagging(input);
 * result.equals(DeleteObjectTaggingOutput.builder()
 *                   .versionId("null")
 *                   .build());
 * }
 *
 * <h3>To remove tag set from an object version</h3>
 *
 * <p>The following example removes tag set associated with the specified object version. The request specifies both the object key and object version.{@snippet :
 * var input = DeleteObjectTaggingInput.builder()
 *                 .bucket("examplebucket").key("HappyFace.jpg").versionId("ydlaNkwWm0SfKJR.T1b1fIdPRbldTYRI")
 *                 .build();
 *
 * var result = client.deleteObjectTagging(input);
 * result.equals(DeleteObjectTaggingOutput.builder()
 *                   .versionId("ydlaNkwWm0SfKJR.T1b1fIdPRbldTYRI")
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class DeleteObjectTagging implements ApiOperation<DeleteObjectTaggingInput, DeleteObjectTaggingOutput> {

    private static final DeleteObjectTagging $INSTANCE = new DeleteObjectTagging();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteObjectTagging"),
            HttpTrait.builder().method("DELETE").code(204).uri(UriPattern.parse("/{Bucket}/{Key+}?tagging")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteObjectTagging instance() {
        return $INSTANCE;
    }

    private DeleteObjectTagging() {}

    @Override
    public ShapeBuilder<DeleteObjectTaggingInput> inputBuilder() {
        return DeleteObjectTaggingInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteObjectTaggingOutput> outputBuilder() {
        return DeleteObjectTaggingOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteObjectTaggingInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteObjectTaggingOutput.$SCHEMA;
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
