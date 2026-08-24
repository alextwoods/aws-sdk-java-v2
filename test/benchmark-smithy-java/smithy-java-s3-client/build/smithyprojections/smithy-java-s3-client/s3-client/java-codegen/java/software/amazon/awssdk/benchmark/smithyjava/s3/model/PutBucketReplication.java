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
 * <p> Creates a replication configuration or replaces an existing one. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication.html">Replication</a> in
 * the <i>Amazon S3 User Guide</i>.
 *
 * <p>Specify the replication configuration in the request body. In the replication configuration, you provide the name
 * of the destination bucket or buckets where you want Amazon S3 to replicate objects, the IAM role that Amazon S3 can
 * assume to replicate objects on your behalf, and other relevant information. You can invoke this request for a
 * specific Amazon Web Services Region by using the <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_condition-keys.html#condition-keys-requestedregion"><code>aws:RequestedRegion</code></a> condition key.
 *
 * <p>A replication configuration must include at least one rule, and can contain a maximum of 1,000. Each rule
 * identifies a subset of objects to replicate by filtering the objects in the source bucket. To choose additional
 * subsets of objects to replicate, add a rule for each subset.
 *
 * <p>To specify a subset of the objects in the source bucket to apply a replication rule to, add the Filter element as
 * a child of the Rule element. You can filter objects based on an object key prefix, one or more object tags, or both.
 * When you add the Filter element in the configuration, you must also add the following elements: <code>
 * DeleteMarkerReplication</code>, <code>Status</code>, and <code>Priority</code>.
 *
 * <p>If you are using an earlier version of the replication configuration, Amazon S3 handles replication of delete
 * markers differently. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication-add-config.html#replication-backward-compat-considerations">Backward Compatibility</a>.
 *
 * <p>For information about enabling versioning on a bucket, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/Versioning.html">Using Versioning</a>.
 *
 * <dl>
 *   <dt>
 *     Handling Replication of Encrypted Objects
 *   </dt>
 *   <dd>
 *
 *     <p>By default, Amazon S3 doesn't replicate objects that are stored at rest using server-side encryption with
 *     KMS keys. To replicate Amazon Web Services KMS-encrypted objects, add the following: <code>
 *     SourceSelectionCriteria</code>, <code>SseKmsEncryptedObjects</code>, <code>Status</code>, <code>
 *     EncryptionConfiguration</code>, and <code>ReplicaKmsKeyID</code>. For information about replication
 *     configuration, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication-config-for-kms-objects.html">Replicating Objects Created with SSE Using KMS keys</a>.
 *
 *     <p>For information on <code>PutBucketReplication</code> errors, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#ReplicationErrorCodeList">List of replication-related error
 *     codes</a>
 *   </dd>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p>To create a <code>PutBucketReplication</code> request, you must have <code>s3:PutReplicationConfiguration</code>
 *     permissions for the bucket.
 *
 *     <pre>{@code
 *       </p>
 *            <p>By default, a resource owner, in this case the Amazon Web Services account that created the bucket, can
 *         perform this operation. The resource owner can also grant others permissions to perform the
 *         operation. For more information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-with-s3-actions.html">Specifying Permissions in a Policy</a>
 *         and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing
 *           Access Permissions to Your Amazon S3 Resources</a>.</p>
 *            <note>
 *               <p>To perform this operation, the user or role performing the action must have the <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_passrole.html">iam:PassRole</a> permission.</p>
 *            </note>
 *         </dd>
 *      </dl>
 *      <p>The following operations are related to <code>PutBucketReplication</code>:</p>
 *      <ul>
 *         <li>
 *            <p>
 *               <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketReplication.html">GetBucketReplication</a>
 *            </p>
 *         </li>
 *         <li>
 *            <p>
 *               <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketReplication.html">DeleteBucketReplication</a>
 *            </p>
 *         </li>
 *      </ul>
 *      <important>
 *         <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my  file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>my%20%20file.txt</code>.</p>
 *      </important>
 *
 *     }</pre>
 *   </dd>
 * </dl>
 *
 * <h2>Examples</h2>
 * <h3>Set replication configuration on a bucket</h3>
 *
 * <p>The following example sets replication configuration on a bucket.{@snippet :
 * var input = PutBucketReplicationInput.builder()
 *                 .bucket("examplebucket").replicationConfiguration(ReplicationConfiguration.builder()
 *                                               .role("arn:aws:iam::123456789012:role/examplerole").rules(List.of(ReplicationRule.builder()
 *                                                                   .prefix("").status(ReplicationRuleStatus.ENABLED).destination(Destination.builder()
 *                                                                                    .bucket("arn:aws:s3:::destinationbucket").storageClass(StorageClass.STANDARD)
 *                                                                                    .build())
 *                                                                   .build()))
 *                                               .build())
 *                 .build();
 *
 * var result = client.putBucketReplication(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketReplication implements ApiOperation<PutBucketReplicationInput, PutBucketReplicationOutput> {

    private static final PutBucketReplication $INSTANCE = new PutBucketReplication();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketReplication"),
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
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?replication")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketReplication instance() {
        return $INSTANCE;
    }

    private PutBucketReplication() {}

    @Override
    public ShapeBuilder<PutBucketReplicationInput> inputBuilder() {
        return PutBucketReplicationInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketReplicationOutput> outputBuilder() {
        return PutBucketReplicationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketReplicationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketReplicationOutput.$SCHEMA;
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
