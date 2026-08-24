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
 * <p>Returns the Cross-Origin Resource Sharing (CORS) configuration information set for the bucket.
 *
 * <p> To use this operation, you must have permission to perform the <code>s3:GetBucketCORS</code> action. By default,
 * the bucket owner has this permission and can grant it to others.
 *
 * <p>When you use this API operation with an access point, provide the alias of the access point in place of the bucket
 * name.
 *
 * <p>When you use this API operation with an Object Lambda access point, provide the alias of the Object Lambda access
 * point in place of the bucket name. If the Object Lambda access point alias in a request is not valid, the error code <code>
 * InvalidAccessPointAliasError</code> is returned. For more information about <code>InvalidAccessPointAliasError</code>
 * , see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#ErrorCodeList">List of Error Codes</a>.
 *
 * <p> For more information about CORS, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/cors.html"> Enabling Cross-Origin Resource Sharing</a>.
 *
 * <p>The following operations are related to <code>GetBucketCors</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketCors.html">PutBucketCors</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketCors.html">DeleteBucketCors</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To get cors configuration set on a bucket</h3>
 *
 * <p>The following example returns cross-origin resource sharing (CORS) configuration set on a bucket.{@snippet :
 * var input = GetBucketCorsInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketCors(input);
 * result.equals(GetBucketCorsOutput.builder()
 *                   .corsRules(List.of(CORSRule.builder()
 *                                           .allowedHeaders(List.of("Authorization")).maxAgeSeconds(3000).allowedMethods(List.of("GET")).allowedOrigins(List.of("*"))
 *                                           .build()))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketCors implements ApiOperation<GetBucketCorsInput, GetBucketCorsOutput> {

    private static final GetBucketCors $INSTANCE = new GetBucketCors();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketCors"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?cors")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketCors instance() {
        return $INSTANCE;
    }

    private GetBucketCors() {}

    @Override
    public ShapeBuilder<GetBucketCorsInput> inputBuilder() {
        return GetBucketCorsInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketCorsOutput> outputBuilder() {
        return GetBucketCorsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketCorsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketCorsOutput.$SCHEMA;
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
