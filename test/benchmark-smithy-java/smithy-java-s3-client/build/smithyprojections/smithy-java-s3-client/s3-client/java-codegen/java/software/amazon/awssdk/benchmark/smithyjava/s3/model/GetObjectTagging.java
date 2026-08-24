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
 * <p>Returns the tag-set of an object. You send the GET request against the tagging subresource associated with the
 * object.
 *
 * <p>To use this operation, you must have permission to perform the <code>s3:GetObjectTagging</code> action. By
 * default, the GET action returns information about current version of an object. For a versioned bucket, you can have
 * multiple versions of an object in your bucket. To retrieve tags of any other version, use the versionId query
 * parameter. You also need permission for the <code>s3:GetObjectVersionTagging</code> action.
 *
 * <p> By default, the bucket owner has this permission and can grant this permission to others.
 *
 * <p> For information about the Amazon S3 object tagging feature, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-tagging.html">Object Tagging</a>.
 *
 * <p>The following actions are related to <code>GetObjectTagging</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjectTagging.html">DeleteObjectTagging</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAttributes.html">GetObjectAttributes</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObjectTagging.html">PutObjectTagging</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To retrieve tag set of a specific object version</h3>
 *
 * <p>The following example retrieves tag set of an object. The request specifies object version.{@snippet :
 * var input = GetObjectTaggingInput.builder()
 *                 .bucket("examplebucket").key("exampleobject").versionId("ydlaNkwWm0SfKJR.T1b1fIdPRbldTYRI")
 *                 .build();
 *
 * var result = client.getObjectTagging(input);
 * result.equals(GetObjectTaggingOutput.builder()
 *                   .versionId("ydlaNkwWm0SfKJR.T1b1fIdPRbldTYRI").tagSet(List.of(Tag.builder()
 *                                        .value("Value1").key("Key1")
 *                                        .build()))
 *                   .build());
 * }
 *
 * <h3>To retrieve tag set of an object</h3>
 *
 * <p>The following example retrieves tag set of an object.{@snippet :
 * var input = GetObjectTaggingInput.builder()
 *                 .bucket("examplebucket").key("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.getObjectTagging(input);
 * result.equals(GetObjectTaggingOutput.builder()
 *                   .versionId("null").tagSet(List.of(
 *                               Tag.builder()
 *                                   .value("Value4").key("Key4")
 *                                   .build()
 *                               ,
 *                               Tag.builder()
 *                                   .value("Value3").key("Key3")
 *                                   .build()
 *                           ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetObjectTagging implements ApiOperation<GetObjectTaggingInput, GetObjectTaggingOutput> {

    private static final GetObjectTagging $INSTANCE = new GetObjectTagging();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetObjectTagging"),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?tagging")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetObjectTagging instance() {
        return $INSTANCE;
    }

    private GetObjectTagging() {}

    @Override
    public ShapeBuilder<GetObjectTaggingInput> inputBuilder() {
        return GetObjectTaggingInput.builder();
    }

    @Override
    public ShapeBuilder<GetObjectTaggingOutput> outputBuilder() {
        return GetObjectTaggingOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetObjectTaggingInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetObjectTaggingOutput.$SCHEMA;
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
