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
 * Deletes the resource-based policy attached to the resource, which can be a table or stream.
 *
 * <p><code>DeleteResourcePolicy</code> is an idempotent operation; running it multiple times on the same resource <i>
 * doesn't</i> result in an error response, unless you specify an <code>ExpectedRevisionId</code>, which will then
 * return a <code>PolicyNotFoundException</code>.
 *
 * <p>To make sure that you don't inadvertently lock yourself out of your own resources, the root principal in your
 * Amazon Web Services account can perform <code>DeleteResourcePolicy</code> requests, even if your resource-based
 * policy explicitly denies the root principal's access.
 *
 * <p><code>DeleteResourcePolicy</code> is an asynchronous operation. If you issue a <code>GetResourcePolicy</code>
 * request immediately after running the <code>DeleteResourcePolicy</code> request, DynamoDB might still return the
 * deleted policy. This is because the policy for your resource might not have been deleted yet. Wait for a few seconds,
 * and then try the <code>GetResourcePolicy</code> request again.
 */
@SmithyGenerated
public final class DeleteResourcePolicy implements ApiOperation<DeleteResourcePolicyInput, DeleteResourcePolicyOutput> {

    private static final DeleteResourcePolicy $INSTANCE = new DeleteResourcePolicy();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DeleteResourcePolicy"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(PolicyNotFoundException.$ID, PolicyNotFoundException.class, PolicyNotFoundException::builder)
        .putType(ResourceInUseException.$ID, ResourceInUseException.class, ResourceInUseException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteResourcePolicy instance() {
        return $INSTANCE;
    }

    private DeleteResourcePolicy() {}

    @Override
    public ShapeBuilder<DeleteResourcePolicyInput> inputBuilder() {
        return DeleteResourcePolicyInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteResourcePolicyOutput> outputBuilder() {
        return DeleteResourcePolicyOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteResourcePolicyInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteResourcePolicyOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, PolicyNotFoundException.$SCHEMA, ResourceInUseException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
