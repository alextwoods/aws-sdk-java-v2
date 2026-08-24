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
 * <p>Sets the <code>cors</code> configuration for your bucket. If the configuration exists, Amazon S3 replaces it.
 *
 * <p>To use this operation, you must be allowed to perform the <code>s3:PutBucketCORS</code> action. By default, the
 * bucket owner has this permission and can grant it to others.
 *
 * <p>You set this configuration on a bucket so that the bucket can service cross-origin requests. For example, you
 * might want to enable a request whose origin is <code>http://www.example.com</code> to access your Amazon S3 bucket at
 * <code>my.example.bucket.com</code> by using the browser's <code>XMLHttpRequest</code> capability.
 *
 * <p>To enable cross-origin resource sharing (CORS) on a bucket, you add the <code>cors</code> subresource to the
 * bucket. The <code>cors</code> subresource is an XML document in which you configure rules that identify origins and
 * the HTTP methods that can be executed on your bucket. The document is limited to 64 KB in size.
 *
 * <p>When Amazon S3 receives a cross-origin request (or a pre-flight OPTIONS request) against a bucket, it evaluates
 * the <code>cors</code> configuration on the bucket and uses the first <code>CORSRule</code> rule that matches the
 * incoming browser request to enable a cross-origin request. For a rule to match, the following conditions must be met:
 *
 * <ul>
 *   <li>
 *     The request's <code>Origin</code> header must match <code>AllowedOrigin</code> elements.
 *   </li>
 *   <li>
 *     The request method (for example, GET, PUT, HEAD, and so on) or the <code>Access-Control-Request-Method</code>
 *     header in case of a pre-flight <code>OPTIONS</code> request must be one of the <code>AllowedMethod</code>
 *     elements.
 *   </li>
 *   <li>
 *     Every header specified in the <code>Access-Control-Request-Headers</code> request header of a pre-flight
 *     request must match an <code>AllowedHeader</code> element.
 *   </li>
 * </ul>
 *
 * <p> For more information about CORS, go to <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/cors.html">Enabling Cross-Origin Resource Sharing</a> in the <i>Amazon S3 User
 * Guide</i>.
 *
 * <p>The following operations are related to <code>PutBucketCors</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketCors.html">GetBucketCors</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketCors.html">DeleteBucketCors</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTOPTIONSobject.html">RESTOPTIONSobject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To set cors configuration on a bucket.</h3>
 *
 * <p>The following example enables PUT, POST, and DELETE requests from www.example.com, and enables GET requests from any domain.{@snippet :
 * var input = PutBucketCorsInput.builder()
 *                 .bucket("").corsConfiguration(CORSConfiguration.builder()
 *                                        .corsRules(List.of(
 *                                                       CORSRule.builder()
 *                                                           .allowedOrigins(List.of("http://www.example.com")).allowedHeaders(List.of("*")).allowedMethods(List.of(
 *                                                                               "PUT",
 *                                                                               "POST",
 *                                                                               "DELETE"
 *                                                                           )).maxAgeSeconds(3000).exposeHeaders(List.of("x-amz-server-side-encryption"))
 *                                                           .build()
 *                                                       ,
 *                                                       CORSRule.builder()
 *                                                           .allowedOrigins(List.of("*")).allowedHeaders(List.of("Authorization")).allowedMethods(List.of("GET")).maxAgeSeconds(3000)
 *                                                           .build()
 *                                                   ))
 *                                        .build()).contentmD5("")
 *                 .build();
 *
 * var result = client.putBucketCors(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketCors implements ApiOperation<PutBucketCorsInput, PutBucketCorsOutput> {

    private static final PutBucketCors $INSTANCE = new PutBucketCors();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketCors"),
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
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?cors")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketCors instance() {
        return $INSTANCE;
    }

    private PutBucketCors() {}

    @Override
    public ShapeBuilder<PutBucketCorsInput> inputBuilder() {
        return PutBucketCorsInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketCorsOutput> outputBuilder() {
        return PutBucketCorsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketCorsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketCorsOutput.$SCHEMA;
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
