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
 * <p>Sets the supplied tag-set to an object that already exists in a bucket. A tag is a key-value pair. For more
 * information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-tagging.html">Object Tagging</a>.
 *
 * <p>You can associate tags with an object by sending a PUT request against the tagging subresource that is associated
 * with the object. You can retrieve tags by sending a GET request. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectTagging.html">GetObjectTagging</a>.
 *
 * <p>For tagging-related restrictions related to characters and encodings, see <a href="https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/allocation-tag-restrictions.html">Tag Restrictions</a>. Note that
 * Amazon S3 limits the maximum number of tags to 10 tags per object.
 *
 * <p>To use this operation, you must have permission to perform the <code>s3:PutObjectTagging</code> action. By
 * default, the bucket owner has this permission and can grant this permission to others.
 *
 * <p>To put tags of any other version, use the <code>versionId</code> query parameter. You also need permission for the
 * <code>s3:PutObjectVersionTagging</code> action.
 *
 * <p><code>PutObjectTagging</code> has the following special errors. For more Amazon S3 errors see, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html">Error Responses</a>
 * .
 *
 * <ul>
 *   <li>
 *     <code>InvalidTag</code> - The tag provided was not a valid tag. This error can occur if the tag did not pass
 *     input validation. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-tagging.html">Object Tagging</a>.
 *   </li>
 *   <li>
 *     <code>MalformedXML</code> - The XML provided does not match the schema.
 *   </li>
 *   <li>
 *     <code>OperationAborted</code> - A conflicting conditional action is currently in progress against this
 *     resource. Please try again.
 *   </li>
 *   <li>
 *     <code>InternalError</code> - The service was unable to apply the provided tag to the object.
 *   </li>
 * </ul>
 *
 * <p>The following operations are related to <code>PutObjectTagging</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectTagging.html">GetObjectTagging</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjectTagging.html">DeleteObjectTagging</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To add tags to an existing object</h3>
 *
 * <p>The following example adds tags to an existing object.{@snippet :
 * var input = PutObjectTaggingInput.builder()
 *                 .bucket("examplebucket").key("HappyFace.jpg").tagging(Tagging.builder()
 *                              .tagSet(List.of(
 *                                          Tag.builder()
 *                                              .key("Key3").value("Value3")
 *                                              .build()
 *                                          ,
 *                                          Tag.builder()
 *                                              .key("Key4").value("Value4")
 *                                              .build()
 *                                      ))
 *                              .build())
 *                 .build();
 *
 * var result = client.putObjectTagging(input);
 * result.equals(PutObjectTaggingOutput.builder()
 *                   .versionId("null")
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class PutObjectTagging implements ApiOperation<PutObjectTaggingInput, PutObjectTaggingOutput> {

    private static final PutObjectTagging $INSTANCE = new PutObjectTagging();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutObjectTagging"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .withMember("requestChecksumRequired", true)
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?tagging")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutObjectTagging instance() {
        return $INSTANCE;
    }

    private PutObjectTagging() {}

    @Override
    public ShapeBuilder<PutObjectTaggingInput> inputBuilder() {
        return PutObjectTaggingInput.builder();
    }

    @Override
    public ShapeBuilder<PutObjectTaggingOutput> outputBuilder() {
        return PutObjectTaggingOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutObjectTaggingInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutObjectTaggingOutput.$SCHEMA;
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
