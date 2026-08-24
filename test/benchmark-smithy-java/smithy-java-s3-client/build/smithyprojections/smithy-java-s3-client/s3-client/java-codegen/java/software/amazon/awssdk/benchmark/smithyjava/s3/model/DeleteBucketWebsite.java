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
 * <p>This action removes the website configuration for a bucket. Amazon S3 returns a <code>200 OK</code> response upon
 * successfully deleting a website configuration on the specified bucket. You will get a <code>200 OK</code> response if
 * the website configuration you are trying to delete does not exist on the bucket. Amazon S3 returns a <code>404</code>
 * response if the bucket specified in the request does not exist.
 *
 * <p>This DELETE action requires the <code>S3:DeleteBucketWebsite</code> permission. By default, only the bucket owner
 * can delete the website configuration attached to a bucket. However, bucket owners can grant other users permission to
 * delete the website configuration by writing a bucket policy granting them the <code>S3:DeleteBucketWebsite</code>
 * permission.
 *
 * <p>For more information about hosting websites, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/WebsiteHosting.html">Hosting Websites on Amazon S3</a>.
 *
 * <p>The following operations are related to <code>DeleteBucketWebsite</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketWebsite.html">GetBucketWebsite</a>
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
 * <h3>To delete bucket website configuration</h3>
 *
 * <p>The following example deletes bucket website configuration.{@snippet :
 * var input = DeleteBucketWebsiteInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.deleteBucketWebsite(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class DeleteBucketWebsite implements ApiOperation<DeleteBucketWebsiteInput, DeleteBucketWebsiteOutput> {

    private static final DeleteBucketWebsite $INSTANCE = new DeleteBucketWebsite();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#DeleteBucketWebsite"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("DELETE").code(204).uri(UriPattern.parse("/{Bucket}?website")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteBucketWebsite instance() {
        return $INSTANCE;
    }

    private DeleteBucketWebsite() {}

    @Override
    public ShapeBuilder<DeleteBucketWebsiteInput> inputBuilder() {
        return DeleteBucketWebsiteInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteBucketWebsiteOutput> outputBuilder() {
        return DeleteBucketWebsiteOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteBucketWebsiteInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteBucketWebsiteOutput.$SCHEMA;
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
