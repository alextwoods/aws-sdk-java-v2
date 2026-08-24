package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Returns the resource-based policy document attached to the resource, which can be a table or stream, in JSON format.
 *
 * <p><code>GetResourcePolicy</code> follows an <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.ReadConsistency.html"><i>eventually consistent</i></a> model. The following list describes
 * the outcomes when you issue the <code>GetResourcePolicy</code> request immediately after issuing another request:
 *
 * <ul>
 *   <li>
 *     If you issue a <code>GetResourcePolicy</code> request immediately after a <code>PutResourcePolicy</code>
 *     request, DynamoDB might return a <code>PolicyNotFoundException</code>.
 *   </li>
 *   <li>
 *     If you issue a <code>GetResourcePolicy</code>request immediately after a <code>DeleteResourcePolicy</code>
 *     request, DynamoDB might return the policy that was present before the deletion request.
 *   </li>
 *   <li>
 *     If you issue a <code>GetResourcePolicy</code> request immediately after a <code>CreateTable</code> request,
 *     which includes a resource-based policy, DynamoDB might return a <code>ResourceNotFoundException</code> or a <code>
 *     PolicyNotFoundException</code>.
 *   </li>
 * </ul>
 *
 * <p>Because <code>GetResourcePolicy</code> uses an <i>eventually consistent</i> query, the metadata for your policy or
 * table might not be available at that moment. Wait for a few seconds, and then retry the <code>GetResourcePolicy</code>
 * request.
 *
 * <p>After a <code>GetResourcePolicy</code> request returns a policy created using the <code>PutResourcePolicy</code>
 * request, the policy will be applied in the authorization of requests to the resource. Because this process is
 * eventually consistent, it will take some time to apply the policy to all requests to a resource. Policies that you
 * attach while creating a table using the <code>CreateTable</code> request will always be applied to all requests for
 * that table.
 */
@SmithyGenerated
public final class GetResourcePolicy implements ApiOperation<GetResourcePolicyInput, GetResourcePolicyOutput> {

    private static final GetResourcePolicy $INSTANCE = new GetResourcePolicy();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#GetResourcePolicy"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(PolicyNotFoundException.$ID, PolicyNotFoundException.class, PolicyNotFoundException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetResourcePolicy instance() {
        return $INSTANCE;
    }

    private GetResourcePolicy() {}

    @Override
    public ShapeBuilder<GetResourcePolicyInput> inputBuilder() {
        return GetResourcePolicyInput.builder();
    }

    @Override
    public ShapeBuilder<GetResourcePolicyOutput> outputBuilder() {
        return GetResourcePolicyOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetResourcePolicyInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetResourcePolicyOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, PolicyNotFoundException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
        return DynamoDBApiService.instance();
    }
    }
