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
 * This operation allows you to perform transactional reads or writes on data stored in DynamoDB, using PartiQL.
 *
 * <p>The entire transaction must consist of either read statements or write statements, you cannot mix both in one
 * transaction. The EXISTS function is an exception and can be used to check the condition of specific attributes of the
 * item in a similar manner to <code>ConditionCheck</code> in the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/transaction-apis.html#transaction-apis-txwriteitems">TransactWriteItems</a> API.
 */
@SmithyGenerated
public final class ExecuteTransaction implements ApiOperation<ExecuteTransactionInput, ExecuteTransactionOutput> {

    private static final ExecuteTransaction $INSTANCE = new ExecuteTransaction();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#ExecuteTransaction"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(IdempotentParameterMismatchException.$ID, IdempotentParameterMismatchException.class, IdempotentParameterMismatchException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(ProvisionedThroughputExceededException.$ID, ProvisionedThroughputExceededException.class, ProvisionedThroughputExceededException::builder)
        .putType(RequestLimitExceeded.$ID, RequestLimitExceeded.class, RequestLimitExceeded::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .putType(ThrottlingException.$ID, ThrottlingException.class, ThrottlingException::builder)
        .putType(TransactionCanceledException.$ID, TransactionCanceledException.class, TransactionCanceledException::builder)
        .putType(TransactionInProgressException.$ID, TransactionInProgressException.class, TransactionInProgressException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema IDEMPOTENCY_TOKEN_MEMBER = ExecuteTransactionInput.$SCHEMA.member("ClientRequestToken");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ExecuteTransaction instance() {
        return $INSTANCE;
    }

    private ExecuteTransaction() {}

    @Override
    public ShapeBuilder<ExecuteTransactionInput> inputBuilder() {
        return ExecuteTransactionInput.builder();
    }

    @Override
    public ShapeBuilder<ExecuteTransactionOutput> outputBuilder() {
        return ExecuteTransactionOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ExecuteTransactionInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ExecuteTransactionOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(IdempotentParameterMismatchException.$SCHEMA, InternalServerError.$SCHEMA, ProvisionedThroughputExceededException.$SCHEMA, RequestLimitExceeded.$SCHEMA, ResourceNotFoundException.$SCHEMA, ThrottlingException.$SCHEMA, TransactionCanceledException.$SCHEMA, TransactionInProgressException.$SCHEMA);
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
        return IDEMPOTENCY_TOKEN_MEMBER;
    }

    @Override
    public ApiService service() {
        return DynamoDBApiService.instance();
    }
    }
