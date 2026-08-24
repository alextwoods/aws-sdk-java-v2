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
 * Attaches a resource-based policy document to the resource, which can be a table or stream. When you attach a
 * resource-based policy using this API, the policy application is <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.ReadConsistency.html"><i>eventually consistent</i></a>.
 *
 * <p><code>PutResourcePolicy</code> is an idempotent operation; running it multiple times on the same resource using
 * the same policy document will return the same revision ID. If you specify an <code>ExpectedRevisionId</code> that
 * doesn't match the current policy's <code>RevisionId</code>, the <code>PolicyNotFoundException</code> will be
 * returned.
 *
 * <p><code>PutResourcePolicy</code> is an asynchronous operation. If you issue a <code>GetResourcePolicy</code> request
 * immediately after a <code>PutResourcePolicy</code> request, DynamoDB might return your previous policy, if there was
 * one, or return the <code>PolicyNotFoundException</code>. This is because <code>GetResourcePolicy</code> uses an
 * eventually consistent query, and the metadata for your policy or table might not be available at that moment. Wait
 * for a few seconds, and then try the <code>GetResourcePolicy</code> request again.
 */
@SmithyGenerated
public final class PutResourcePolicy implements ApiOperation<PutResourcePolicyInput, PutResourcePolicyOutput> {

    private static final PutResourcePolicy $INSTANCE = new PutResourcePolicy();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#PutResourcePolicy"));

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
    public static PutResourcePolicy instance() {
        return $INSTANCE;
    }

    private PutResourcePolicy() {}

    @Override
    public ShapeBuilder<PutResourcePolicyInput> inputBuilder() {
        return PutResourcePolicyInput.builder();
    }

    @Override
    public ShapeBuilder<PutResourcePolicyOutput> outputBuilder() {
        return PutResourcePolicyOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutResourcePolicyInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutResourcePolicyOutput.$SCHEMA;
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
