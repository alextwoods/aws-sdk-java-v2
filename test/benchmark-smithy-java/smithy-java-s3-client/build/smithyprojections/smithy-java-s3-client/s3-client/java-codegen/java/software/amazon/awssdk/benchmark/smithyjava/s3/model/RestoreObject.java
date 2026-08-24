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
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Restores an archived copy of an object back into Amazon S3
 *
 * <p>This functionality is not supported for Amazon S3 on Outposts.
 *
 * <p>This action performs the following types of requests:
 *
 * <ul>
 *   <li>
 *     <code>restore an archive</code> - Restore an archived object
 *   </li>
 * </ul>
 *
 * <p>For more information about the <code>S3</code> structure in the request body, see the following:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/S3_ACLs_UsingACLs.html">Managing Access with ACLs</a> in the <i>Amazon S3 User Guide</i>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/serv-side-encryption.html">Protecting Data Using Server-Side Encryption</a> in the <i>Amazon S3 User Guide</i>
 *   </li>
 * </ul>
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p>To use this operation, you must have permissions to perform the <code>s3:RestoreObject</code> action. The
 *     bucket owner has this permission by default and can grant this permission to others. For more information
 *     about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-actions.html#using-with-s3-actions-related-to-bucket-subresources">Permissions Related to Bucket Subresource Operations</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing Access
 *     Permissions to Your Amazon S3 Resources</a> in the <i>Amazon S3 User Guide</i>.
 *   </dd>
 *   <dt>
 *     Restoring objects
 *   </dt>
 *   <dd>
 *
 *     <p>Objects that you archive to the S3 Glacier Flexible Retrieval or S3 Glacier Deep Archive storage class,
 *     and S3 Intelligent-Tiering Archive or S3 Intelligent-Tiering Deep Archive tiers, are not accessible in real
 *     time. For objects in the S3 Glacier Flexible Retrieval or S3 Glacier Deep Archive storage classes, you must
 *     first initiate a restore request, and then wait until a temporary copy of the object is available. If you
 *     want a permanent copy of the object, create a copy of it in the Amazon S3 Standard storage class in your S3
 *     bucket. To access an archived object, you must restore the object for the duration (number of days) that you
 *     specify. For objects in the Archive Access or Deep Archive Access tiers of S3 Intelligent-Tiering, you must
 *     first initiate a restore request, and then wait until the object is moved into the Frequent Access tier.
 *
 *     <p>To restore a specific object version, you can provide a version ID. If you don't provide a version ID,
 *     Amazon S3 restores the current version.
 *
 *     <p>When restoring an archived object, you can specify one of the following data access tier options in the <code>
 *     Tier</code> element of the request body:
 *
 *     <ul>
 *       <li>
 *         <code>Expedited</code> - Expedited retrievals allow you to quickly access your data stored in the S3
 *         Glacier Flexible Retrieval storage class or S3 Intelligent-Tiering Archive tier when occasional
 *         urgent requests for restoring archives are required. For all but the largest archived objects (250
 *         MB+), data accessed using Expedited retrievals is typically made available within 1–5 minutes.
 *         Provisioned capacity ensures that retrieval capacity for Expedited retrievals is available when you
 *         need it. Expedited retrievals and provisioned capacity are not available for objects stored in the S3
 *         Glacier Deep Archive storage class or S3 Intelligent-Tiering Deep Archive tier.
 *       </li>
 *       <li>
 *         <code>Standard</code> - Standard retrievals allow you to access any of your archived objects within
 *         several hours. This is the default option for retrieval requests that do not specify the retrieval
 *         option. Standard retrievals typically finish within 3–5 hours for objects stored in the S3 Glacier
 *         Flexible Retrieval storage class or S3 Intelligent-Tiering Archive tier. They typically finish within
 *         12 hours for objects stored in the S3 Glacier Deep Archive storage class or S3 Intelligent-Tiering
 *         Deep Archive tier. Standard retrievals are free for objects stored in S3 Intelligent-Tiering.
 *       </li>
 *       <li>
 *         <code>Bulk</code> - Bulk retrievals free for objects stored in the S3 Glacier Flexible Retrieval and
 *         S3 Intelligent-Tiering storage classes, enabling you to retrieve large amounts, even petabytes, of
 *         data at no cost. Bulk retrievals typically finish within 5–12 hours for objects stored in the S3
 *         Glacier Flexible Retrieval storage class or S3 Intelligent-Tiering Archive tier. Bulk retrievals are
 *         also the lowest-cost retrieval option when restoring objects from S3 Glacier Deep Archive. They
 *         typically finish within 48 hours for objects stored in the S3 Glacier Deep Archive storage class or
 *         S3 Intelligent-Tiering Deep Archive tier.
 *       </li>
 *     </ul>
 *
 *     <p>For more information about archive retrieval options and provisioned capacity for <code>Expedited</code>
 *     data access, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/restoring-objects.html">Restoring Archived Objects</a> in the <i>Amazon S3 User Guide</i>.
 *
 *     <p>You can use Amazon S3 restore speed upgrade to change the restore speed to a faster speed while it is in
 *     progress. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/restoring-objects.html#restoring-objects-upgrade-tier.title.html"> Upgrading the speed of an in-progress restore</a> in the <i>Amazon S3
 *     User Guide</i>.
 *
 *     <p>To get the status of object restoration, you can send a <code>HEAD</code> request. Operations return the <code>
 *     x-amz-restore</code> header, which provides information about the restoration status, in the response. You
 *     can use Amazon S3 event notifications to notify you when a restore is initiated or completed. For more
 *     information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/NotificationHowTo.html">Configuring Amazon S3 Event Notifications</a> in the <i>Amazon S3 User Guide</i>.
 *
 *     <p>After restoring an archived object, you can update the restoration period by reissuing the request with a
 *     new period. Amazon S3 updates the restoration period relative to the current time and charges only for the
 *     request-there are no data transfer charges. You cannot update the restoration period when Amazon S3 is
 *     actively processing your current restore request for the object.
 *
 *     <p>If your bucket has a lifecycle configuration with a rule that includes an expiration action, the object
 *     expiration overrides the life span that you specify in a restore request. For example, if you restore an
 *     object copy for 10 days, but the object is scheduled to expire in 3 days, Amazon S3 deletes the object in 3
 *     days. For more information about lifecycle configuration, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycleConfiguration.html">PutBucketLifecycleConfiguration</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lifecycle-mgmt.html">
 *     Object Lifecycle Management</a> in <i>Amazon S3 User Guide</i>.
 *   </dd>
 *   <dt>
 *     Responses
 *   </dt>
 *   <dd>
 *
 *     <p>A successful action returns either the <code>200 OK</code> or <code>202 Accepted</code> status code.
 *
 *     <ul>
 *       <li>
 *         If the object is not previously restored, then Amazon S3 returns <code>202 Accepted</code> in the
 *         response.
 *       </li>
 *       <li>
 *         If the object is previously restored, Amazon S3 returns <code>200 OK</code> in the response.
 *       </li>
 *     </ul>
 *
 *     <ul>
 *       <li>
 *         Special errors:
 *
 *         <ul>
 *           <li>
 *             <i>Code: RestoreAlreadyInProgress</i>
 *           </li>
 *           <li>
 *             <i>Cause: Object restore is already in progress.</i>
 *           </li>
 *           <li>
 *             <i>HTTP Status Code: 409 Conflict</i>
 *           </li>
 *           <li>
 *             <i>SOAP Fault Code Prefix: Client</i>
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *
 *         <ul>
 *           <li>
 *             <i>Code: GlacierExpeditedRetrievalNotAvailable</i>
 *           </li>
 *           <li>
 *             <i>Cause: expedited retrievals are currently not available. Try again later. (Returned if
 *             there is insufficient capacity to process the Expedited request. This error applies only to
 *             Expedited retrievals and not to S3 Standard or Bulk retrievals.)</i>
 *           </li>
 *           <li>
 *             <i>HTTP Status Code: 503</i>
 *           </li>
 *           <li>
 *             <i>SOAP Fault Code Prefix: N/A</i>
 *           </li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>RestoreObject</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycleConfiguration.html">PutBucketLifecycleConfiguration</a>
 *   </li>
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
 * <h3>To restore an archived object</h3>
 *
 * <p>The following example restores for one day an archived copy of an object back into Amazon S3 bucket.{@snippet :
 * var input = RestoreObjectInput.builder()
 *                 .bucket("examplebucket").key("archivedobjectkey").restoreRequest(RestoreRequest.builder()
 *                                     .days(1).glacierJobParameters(GlacierJobParameters.builder()
 *                                                               .tier(Tier.EXPEDITED)
 *                                                               .build())
 *                                     .build())
 *                 .build();
 *
 * var result = client.restoreObject(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class RestoreObject implements ApiOperation<RestoreObjectInput, RestoreObjectOutput> {

    private static final RestoreObject $INSTANCE = new RestoreObject();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#RestoreObject"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .build()
            ),
            HttpTrait.builder().method("POST").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?restore")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ObjectAlreadyInActiveTierError.$ID, ObjectAlreadyInActiveTierError.class, ObjectAlreadyInActiveTierError::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static RestoreObject instance() {
        return $INSTANCE;
    }

    private RestoreObject() {}

    @Override
    public ShapeBuilder<RestoreObjectInput> inputBuilder() {
        return RestoreObjectInput.builder();
    }

    @Override
    public ShapeBuilder<RestoreObjectOutput> outputBuilder() {
        return RestoreObjectOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return RestoreObjectInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return RestoreObjectOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ObjectAlreadyInActiveTierError.$SCHEMA);
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
