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
 * End of support notice: As of October 1, 2025, Amazon S3 has discontinued support for Email Grantee Access Control
 * Lists (ACLs). If you attempt to use an Email Grantee ACL in a request after October 1, 2025, the request will receive
 * an <code>HTTP 405</code> (Method Not Allowed) error.
 *
 * <p>This change affects the following Amazon Web Services Regions: US East (N. Virginia), US West (N. California), US
 * West (Oregon), Asia Pacific (Singapore), Asia Pacific (Sydney), Asia Pacific (Tokyo), Europe (Ireland), and South
 * America (São Paulo).
 *
 * <p>This operation is not supported for directory buckets.
 *
 * <p>Set the logging parameters for a bucket and to specify permissions for who can view and modify the logging
 * parameters. All logs are saved to buckets in the same Amazon Web Services Region as the source bucket. To set the
 * logging status of a bucket, you must be the bucket owner.
 *
 * <p>The bucket owner is automatically granted FULL_CONTROL to all logs. You use the <code>Grantee</code> request
 * element to grant access to other people. The <code>Permissions</code> request element specifies the kind of access
 * the grantee has to the logs.
 *
 * <p>If the target bucket for log delivery uses the bucket owner enforced setting for S3 Object Ownership, you can't
 * use the <code>Grantee</code> request element to grant access to others. Permissions can only be granted using
 * policies. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/enable-server-access-logging.html#grant-log-delivery-permissions-general">Permissions for server access log delivery</a> in the <i>Amazon S3 User Guide</i>
 * .
 *
 * <dl>
 *   <dt>
 *     Grantee Values
 *   </dt>
 *   <dd>
 *
 *     <p>You can specify the person (grantee) to whom you're assigning access rights (by using request elements) in
 *     the following ways. For examples of how to specify these grantee values in JSON format, see the Amazon Web
 *     Services CLI example in <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/enable-server-access-logging.html"> Enabling Amazon S3 server access logging</a> in the <i>Amazon S3 User Guide</i>.
 *
 *     <ul>
 *       <li>
 *         By the person's ID:<code>&lt;Grantee xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *         xsi:type="CanonicalUser"&gt;&lt;ID&gt;&lt;&gt;ID&lt;&gt;
 *         &lt;DisplayName&gt;&lt;&gt;GranteesEmail&lt;&gt;</code><code>DisplayName</code> is optional and
 *         ignored in the request.
 *       </li>
 *       <li>
 *         By Email address:<code> &lt;Grantee xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *         xsi:type="AmazonCustomerByEmail"&gt;&lt;EmailAddress&gt;&lt;&gt;Grantees{@literal @}email.com&lt;&gt;</code>
 *         The grantee is resolved to the <code>CanonicalUser</code> and, in a response to a <code>GETObjectAcl</code>
 *         request, appears as the CanonicalUser.
 *       </li>
 *       <li>
 *         By URI:<code>&lt;Grantee xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *         xsi:type="Group"&gt;&lt;URI&gt;&lt;&gt;http://acs.amazonaws.com/groups/global/AuthenticatedUsers&lt;&gt;</code>
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <p>To enable logging, you use <code>LoggingEnabled</code> and its children request elements. To disable logging, you
 * use an empty <code>BucketLoggingStatus</code> request element:
 *
 * <p><code>&lt;BucketLoggingStatus xmlns="http://doc.s3.amazonaws.com/2006-03-01" /&gt;</code>
 *
 * <p>For more information about server access logging, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ServerLogs.html">Server Access Logging</a> in the <i>Amazon S3 User Guide</i>
 * .
 *
 * <p>For more information about creating a bucket, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>. For more information about returning the
 * logging status of a bucket, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLogging.html">GetBucketLogging</a>.
 *
 * <p>The following operations are related to <code>PutBucketLogging</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html">DeleteBucket</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLogging.html">GetBucketLogging</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>Set logging configuration for a bucket</h3>
 *
 * <p>The following example sets logging policy on a bucket. For the Log Delivery group to deliver logs to the destination bucket, it needs permission for the READ_ACP action which the policy grants.{@snippet :
 * var input = PutBucketLoggingInput.builder()
 *                 .bucket("sourcebucket").bucketLoggingStatus(BucketLoggingStatus.builder()
 *                                          .loggingEnabled(LoggingEnabled.builder()
 *                                                              .targetBucket("targetbucket").targetPrefix("MyBucketLogs/").targetGrants(List.of(TargetGrant.builder()
 *                                                                                         .grantee(Grantee.builder()
 *                                                                                                      .type(Type.GROUP).uri("http://acs.amazonaws.com/groups/global/AllUsers")
 *                                                                                                      .build()).permission(BucketLogsPermission.READ)
 *                                                                                         .build()))
 *                                                              .build())
 *                                          .build())
 *                 .build();
 *
 * var result = client.putBucketLogging(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketLogging implements ApiOperation<PutBucketLoggingInput, PutBucketLoggingOutput> {

    private static final PutBucketLogging $INSTANCE = new PutBucketLogging();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketLogging"),
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
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?logging")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketLogging instance() {
        return $INSTANCE;
    }

    private PutBucketLogging() {}

    @Override
    public ShapeBuilder<PutBucketLoggingInput> inputBuilder() {
        return PutBucketLoggingInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketLoggingOutput> outputBuilder() {
        return PutBucketLoggingOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketLoggingInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketLoggingOutput.$SCHEMA;
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
