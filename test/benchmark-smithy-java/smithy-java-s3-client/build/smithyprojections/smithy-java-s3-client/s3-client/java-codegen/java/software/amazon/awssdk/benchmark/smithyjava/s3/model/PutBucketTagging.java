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
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Sets the tags for a general purpose bucket if attribute based access control (ABAC) is not enabled for the bucket.
 * When you <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/buckets-tagging-enable-abac.html">enable ABAC for a general purpose bucket</a>, you can no longer use this operation for that bucket and
 * must use the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_control_TagResource.html">TagResource</a> or <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_control_UntagResource.html">UntagResource</a> operations instead.
 *
 * <p>Use tags to organize your Amazon Web Services bill to reflect your own cost structure. To do this, sign up to get
 * your Amazon Web Services account bill with tag key values included. Then, to see the cost of combined resources,
 * organize your billing information according to resources with the same tag key values. For example, you can tag
 * several resources with a specific application name, and then organize your billing information to see the total cost
 * of that application across several services. For more information, see <a href="https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/cost-alloc-tags.html">Cost Allocation and Tagging</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/CostAllocTagging.html">
 * Using Cost Allocation in Amazon S3 Bucket Tags</a>.
 *
 * <p> When this operation sets the tags for a bucket, it will overwrite any current tags the bucket already has. You
 * cannot use this operation to add tags to an existing list of tags.
 *
 * <p>To use this operation, you must have permissions to perform the <code>s3:PutBucketTagging</code> action. The
 * bucket owner has this permission by default and can grant this permission to others. For more information about
 * permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-actions.html#using-with-s3-actions-related-to-bucket-subresources">Permissions Related to Bucket Subresource Operations</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing Access Permissions to
 * Your Amazon S3 Resources</a>.
 *
 * <p><code>PutBucketTagging</code> has the following special errors. For more Amazon S3 errors see, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html">Error Responses</a>
 * .
 *
 * <ul>
 *   <li>
 *     <code>InvalidTag</code> - The tag provided was not a valid tag. This error can occur if the tag did not pass
 *     input validation. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/CostAllocTagging.html">Using Cost Allocation in Amazon S3 Bucket Tags</a>.
 *   </li>
 *   <li>
 *     <code>MalformedXML</code> - The XML provided does not match the schema.
 *   </li>
 *   <li>
 *     <code>OperationAborted</code> - A conflicting conditional action is currently in progress against this
 *     resource. Please try again.
 *   </li>
 *   <li>
 *     <code>InternalError</code> - The service was unable to apply the provided tag to the bucket.
 *   </li>
 * </ul>
 *
 * <p>The following operations are related to <code>PutBucketTagging</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketTagging.html">GetBucketTagging</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketTagging.html">DeleteBucketTagging</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>Set tags on a bucket</h3>
 *
 * <p>The following example sets tags on a bucket. Any existing tags are replaced.{@snippet :
 * var input = PutBucketTaggingInput.builder()
 *                 .bucket("examplebucket").tagging(Tagging.builder()
 *                              .tagSet(List.of(
 *                                          Tag.builder()
 *                                              .key("Key1").value("Value1")
 *                                              .build()
 *                                          ,
 *                                          Tag.builder()
 *                                              .key("Key2").value("Value2")
 *                                              .build()
 *                                      ))
 *                              .build())
 *                 .build();
 *
 * var result = client.putBucketTagging(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketTagging implements ApiOperation<PutBucketTaggingInput, PutBucketTaggingOutput> {

    private static final PutBucketTagging $INSTANCE = new PutBucketTagging();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketTagging"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .withMember("requestChecksumRequired", true)
                    .build()
            ),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?tagging")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketTagging instance() {
        return $INSTANCE;
    }

    private PutBucketTagging() {}

    @Override
    public ShapeBuilder<PutBucketTaggingInput> inputBuilder() {
        return PutBucketTaggingInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketTaggingOutput> outputBuilder() {
        return PutBucketTaggingOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketTaggingInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketTaggingOutput.$SCHEMA;
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
