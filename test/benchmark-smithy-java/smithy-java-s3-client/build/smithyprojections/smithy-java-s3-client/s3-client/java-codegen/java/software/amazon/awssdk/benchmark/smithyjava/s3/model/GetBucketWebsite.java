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
 * <p>Returns the website configuration for a bucket. To host website on Amazon S3, you can configure a bucket as
 * website by adding a website configuration. For more information about hosting websites, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/WebsiteHosting.html">Hosting Websites on
 * Amazon S3</a>.
 *
 * <p>This GET action requires the <code>S3:GetBucketWebsite</code> permission. By default, only the bucket owner can
 * read the bucket website configuration. However, bucket owners can allow other users to read the website configuration
 * by writing a bucket policy granting them the <code>S3:GetBucketWebsite</code> permission.
 *
 * <p>The following operations are related to <code>GetBucketWebsite</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketWebsite.html">DeleteBucketWebsite</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketWebsite.html">PutBucketWebsite</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To get bucket website configuration</h3>
 *
 * <p>The following example retrieves website configuration of a bucket.{@snippet :
 * var input = GetBucketWebsiteInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketWebsite(input);
 * result.equals(GetBucketWebsiteOutput.builder()
 *                   .indexDocument(IndexDocument.builder()
 *                                      .suffix("index.html")
 *                                      .build()).errorDocument(ErrorDocument.builder()
 *                                      .key("error.html")
 *                                      .build())
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketWebsite implements ApiOperation<GetBucketWebsiteInput, GetBucketWebsiteOutput> {

    private static final GetBucketWebsite $INSTANCE = new GetBucketWebsite();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketWebsite"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?website")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketWebsite instance() {
        return $INSTANCE;
    }

    private GetBucketWebsite() {}

    @Override
    public ShapeBuilder<GetBucketWebsiteInput> inputBuilder() {
        return GetBucketWebsiteInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketWebsiteOutput> outputBuilder() {
        return GetBucketWebsiteOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketWebsiteInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketWebsiteOutput.$SCHEMA;
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
