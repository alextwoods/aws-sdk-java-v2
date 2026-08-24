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
 * <p>Returns the notification configuration of a bucket.
 *
 * <p>If notifications are not enabled on the bucket, the action returns an empty <code>NotificationConfiguration</code>
 * element.
 *
 * <p>By default, you must be the bucket owner to read the notification configuration of a bucket. However, the bucket
 * owner can use a bucket policy to grant permission to other users to read this configuration with the <code>
 * s3:GetBucketNotification</code> permission.
 *
 * <p>When you use this API operation with an access point, provide the alias of the access point in place of the bucket
 * name.
 *
 * <p>When you use this API operation with an Object Lambda access point, provide the alias of the Object Lambda access
 * point in place of the bucket name. If the Object Lambda access point alias in a request is not valid, the error code <code>
 * InvalidAccessPointAliasError</code> is returned. For more information about <code>InvalidAccessPointAliasError</code>
 * , see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#ErrorCodeList">List of Error Codes</a>.
 *
 * <p>For more information about setting and reading the notification configuration on a bucket, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">Setting Up
 * Notification of Bucket Events</a>. For more information about bucket policies, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-iam-policies.html">Using Bucket Policies</a>.
 *
 * <p>The following action is related to <code>GetBucketNotification</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketNotification.html">PutBucketNotification</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class GetBucketNotificationConfiguration implements ApiOperation<GetBucketNotificationConfigurationInput, GetBucketNotificationConfigurationOutput> {

    private static final GetBucketNotificationConfiguration $INSTANCE = new GetBucketNotificationConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketNotificationConfiguration"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?notification")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketNotificationConfiguration instance() {
        return $INSTANCE;
    }

    private GetBucketNotificationConfiguration() {}

    @Override
    public ShapeBuilder<GetBucketNotificationConfigurationInput> inputBuilder() {
        return GetBucketNotificationConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketNotificationConfigurationOutput> outputBuilder() {
        return GetBucketNotificationConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketNotificationConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketNotificationConfigurationOutput.$SCHEMA;
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
