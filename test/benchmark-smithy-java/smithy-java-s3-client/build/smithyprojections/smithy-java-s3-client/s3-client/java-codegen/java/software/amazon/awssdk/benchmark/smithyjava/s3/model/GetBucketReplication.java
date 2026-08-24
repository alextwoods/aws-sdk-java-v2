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
 * <p>Returns the replication configuration of a bucket.
 *
 * <p> It can take a while to propagate the put or delete a replication configuration to all Amazon S3 systems.
 * Therefore, a get request soon after put or delete can return a wrong result.
 *
 * <p> For information about replication configuration, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/replication.html">Replication</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <p>This action requires permissions for the <code>s3:GetReplicationConfiguration</code> action. For more information
 * about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-iam-policies.html">Using Bucket Policies and User Policies</a>.
 *
 * <p>If you include the <code>Filter</code> element in a replication configuration, you must also include the <code>
 * DeleteMarkerReplication</code> and <code>Priority</code> elements. The response also returns those elements.
 *
 * <p>For information about <code>GetBucketReplication</code> errors, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#ReplicationErrorCodeList">List of replication-related error codes</a>
 *
 * <p>The following operations are related to <code>GetBucketReplication</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketReplication.html">PutBucketReplication</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucketReplication.html">DeleteBucketReplication</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To get replication configuration set on a bucket</h3>
 *
 * <p>The following example returns replication configuration set on a bucket.{@snippet :
 * var input = GetBucketReplicationInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.getBucketReplication(input);
 * result.equals(GetBucketReplicationOutput.builder()
 *                   .replicationConfiguration(ReplicationConfiguration.builder()
 *                                                 .rules(List.of(ReplicationRule.builder()
 *                                                                     .status(ReplicationRuleStatus.ENABLED).prefix("Tax").destination(Destination.builder()
 *                                                                                      .bucket("arn:aws:s3:::destination-bucket")
 *                                                                                      .build()).id("MWIwNTkwZmItMTE3MS00ZTc3LWJkZDEtNzRmODQwYzc1OTQy")
 *                                                                     .build())).role("arn:aws:iam::acct-id:role/example-role")
 *                                                 .build())
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class GetBucketReplication implements ApiOperation<GetBucketReplicationInput, GetBucketReplicationOutput> {

    private static final GetBucketReplication $INSTANCE = new GetBucketReplication();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetBucketReplication"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?replication")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetBucketReplication instance() {
        return $INSTANCE;
    }

    private GetBucketReplication() {}

    @Override
    public ShapeBuilder<GetBucketReplicationInput> inputBuilder() {
        return GetBucketReplicationInput.builder();
    }

    @Override
    public ShapeBuilder<GetBucketReplicationOutput> outputBuilder() {
        return GetBucketReplicationOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetBucketReplicationInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetBucketReplicationOutput.$SCHEMA;
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
