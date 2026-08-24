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
 * <p>Returns the tag set associated with the general purpose bucket.
 *
 * <p>To use this operation, you must have permission to perform the <code>s3:GetBucketTagging</code> action. By
 * default, the bucket owner has this permission and can grant this permission to others.
 *
 * <p><code>GetBucketTagging</code> has the following special error:
 *
 * <ul>
 *   <li>
 *     Error code: <code>NoSuchTagSet</code>
 *
 *     <ul>
 *       <li>
 *         Description: There is no tag set associated with the bucket.
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>The following operations are related to <code>GetBucketTagging</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketTagging.html">PutBucketTagging</a>
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
 * <h3>To get tag set associated with a bucket</h3>
 *
 * <p>The following example returns tag set associated with a bucket{@snippet :
 * var input = GetBucketTaggingInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketTagging(input);
 * result.equals(GetBucketTaggingOutput.builder()
 *                   .tagSet(List.of(
 *                               Tag.builder()
 *                                   .value("value1").key("key1")
 *                                   .build()
 *                               ,
 *                               Tag.builder()
 *                                   .value("value2").key("key2")
 *                                   .build()
 *                           ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketTagging implements ApiOperation<GetBucketTaggingInput, GetBucketTaggingOutput> {

    private static final GetBucketTagging $INSTANCE = new GetBucketTagging();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketTagging"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?tagging")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketTagging instance() {
        return $INSTANCE;
    }

    private GetBucketTagging() {}

    @Override
    public ShapeBuilder<GetBucketTaggingInput> inputBuilder() {
        return GetBucketTaggingInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketTaggingOutput> outputBuilder() {
        return GetBucketTaggingOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketTaggingInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketTaggingOutput.$SCHEMA;
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
