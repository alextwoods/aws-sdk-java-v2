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
 * <p>Enables notifications of specified events for a bucket. For more information about event notifications, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">
 * Configuring Event Notifications</a>.
 *
 * <p>Using this API, you can replace an existing notification configuration. The configuration is an XML file that
 * defines the event types that you want Amazon S3 to publish and the destination where you want Amazon S3 to publish an
 * event notification when it detects an event of the specified type.
 *
 * <p>By default, your bucket has no event notifications configured. That is, the notification configuration will be an
 * empty <code>NotificationConfiguration</code>.
 *
 * <p><code>&lt;NotificationConfiguration&gt;</code>
 *
 * <p><code></code>
 *
 * <p>This action replaces the existing notification configuration with the configuration you include in the request
 * body.
 *
 * <p>After Amazon S3 receives this request, it first verifies that any Amazon Simple Notification Service (Amazon SNS)
 * or Amazon Simple Queue Service (Amazon SQS) destination exists, and that the bucket owner has permission to publish
 * to it by sending a test notification. In the case of Lambda destinations, Amazon S3 verifies that the Lambda function
 * permissions grant Amazon S3 permission to invoke the function from the Amazon S3 bucket. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">
 * Configuring Notifications for Amazon S3 Events</a>.
 *
 * <p>You can disable notifications by adding the empty NotificationConfiguration element.
 *
 * <p>For more information about the number of event notification configurations that you can create per bucket, see <a href="https://docs.aws.amazon.com/general/latest/gr/s3.html#limits_s3">
 * Amazon S3 service quotas</a> in <i>Amazon Web Services General Reference</i>.
 *
 * <p>By default, only the bucket owner can configure notifications on a bucket. However, bucket owners can use a bucket
 * policy to grant permission to other users to set this configuration with the required <code>s3:PutBucketNotification</code>
 * permission.
 *
 * <p>The PUT notification is an atomic operation. For example, suppose your notification configuration includes SNS
 * topic, SQS queue, and Lambda function configurations. When you send a PUT request with this configuration, Amazon S3
 * sends test messages to your SNS topic. If the message fails, the entire PUT action will fail, and Amazon S3 will not
 * add the configuration to your bucket.
 *
 * <p>If the configuration in the request body includes only one <code>TopicConfiguration</code> specifying only the <code>
 * s3:ReducedRedundancyLostObject</code> event type, the response will also include the <code>x-amz-sns-test-message-id</code>
 * header containing the message ID of the test notification sent to the topic.
 *
 * <p>The following action is related to <code>PutBucketNotificationConfiguration</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketNotificationConfiguration.html">GetBucketNotificationConfiguration</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>Set notification configuration for a bucket</h3>
 *
 * <p>The following example sets notification configuration on a bucket to publish the object created events to an SNS topic.{@snippet :
 * var input = PutBucketNotificationConfigurationInput.builder()
 *                 .bucket("examplebucket").notificationConfiguration(NotificationConfiguration.builder()
 *                                                .topicConfigurations(List.of(TopicConfiguration.builder()
 *                                                                                  .topicArn("arn:aws:sns:us-west-2:123456789012:s3-notification-topic").events(List.of(Event.S3_OBJECT_CREATED_))
 *                                                                                  .build()))
 *                                                .build())
 *                 .build();
 *
 * var result = client.putBucketNotificationConfiguration(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketNotificationConfiguration implements ApiOperation<PutBucketNotificationConfigurationInput, PutBucketNotificationConfigurationOutput> {

    private static final PutBucketNotificationConfiguration $INSTANCE = new PutBucketNotificationConfiguration();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketNotificationConfiguration"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?notification")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketNotificationConfiguration instance() {
        return $INSTANCE;
    }

    private PutBucketNotificationConfiguration() {}

    @Override
    public ShapeBuilder<PutBucketNotificationConfigurationInput> inputBuilder() {
        return PutBucketNotificationConfigurationInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketNotificationConfigurationOutput> outputBuilder() {
        return PutBucketNotificationConfigurationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketNotificationConfigurationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketNotificationConfigurationOutput.$SCHEMA;
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
