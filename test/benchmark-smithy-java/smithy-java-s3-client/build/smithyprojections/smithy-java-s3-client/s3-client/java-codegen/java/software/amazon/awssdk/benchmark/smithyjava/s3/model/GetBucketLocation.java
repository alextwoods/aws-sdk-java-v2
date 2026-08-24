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
 * Using the <code>GetBucketLocation</code> operation is no longer a best practice. To return the Region that a bucket
 * resides in, we recommend that you use the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html">HeadBucket</a> operation instead. For backward compatibility, Amazon S3
 * continues to support the <code>GetBucketLocation</code> operation.
 *
 * <p>Returns the Region the bucket resides in. You set the bucket's Region using the <code>LocationConstraint</code>
 * request parameter in a <code>CreateBucket</code> request. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>.
 *
 * <p>In a bucket's home Region, calls to the <code>GetBucketLocation</code> operation are governed by the bucket's
 * policy. In other Regions, the bucket policy doesn't apply, which means that cross-account access won't be authorized.
 * However, calls to the <code>HeadBucket</code> operation always return the bucket’s location through an HTTP response
 * header, whether access to the bucket is authorized or not. Therefore, we recommend using the <code>HeadBucket</code>
 * operation for bucket Region discovery and to avoid using the <code>GetBucketLocation</code> operation.
 *
 * <p>When you use this API operation with an access point, provide the alias of the access point in place of the bucket
 * name.
 *
 * <p>When you use this API operation with an Object Lambda access point, provide the alias of the Object Lambda access
 * point in place of the bucket name. If the Object Lambda access point alias in a request is not valid, the error code <code>
 * InvalidAccessPointAliasError</code> is returned. For more information about <code>InvalidAccessPointAliasError</code>
 * , see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#ErrorCodeList">List of Error Codes</a>.
 *
 * <p>This operation is not supported for directory buckets.
 *
 * <p>The following operations are related to <code>GetBucketLocation</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To get bucket location</h3>
 *
 * <p>The following example returns bucket location.{@snippet :
 * var input = GetBucketLocationInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketLocation(input);
 * result.equals(GetBucketLocationOutput.builder()
 *                   .locationConstraint(BucketLocationConstraint.US_WEST_2)
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketLocation implements ApiOperation<GetBucketLocationInput, GetBucketLocationOutput> {

    private static final GetBucketLocation $INSTANCE = new GetBucketLocation();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketLocation"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?location")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketLocation instance() {
        return $INSTANCE;
    }

    private GetBucketLocation() {}

    @Override
    public ShapeBuilder<GetBucketLocationInput> inputBuilder() {
        return GetBucketLocationInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketLocationOutput> outputBuilder() {
        return GetBucketLocationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketLocationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketLocationOutput.$SCHEMA;
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
