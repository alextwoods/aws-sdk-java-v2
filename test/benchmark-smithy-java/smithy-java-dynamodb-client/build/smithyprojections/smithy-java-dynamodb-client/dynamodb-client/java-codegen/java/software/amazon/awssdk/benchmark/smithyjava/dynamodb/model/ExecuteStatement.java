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
 * This operation allows you to perform reads and singleton writes on data stored in DynamoDB, using PartiQL.
 *
 * <p>For PartiQL reads (<code>SELECT</code> statement), if the total number of processed items exceeds the maximum
 * dataset size limit of 1 MB, the read stops and results are returned to the user as a <code>LastEvaluatedKey</code>
 * value to continue the read in a subsequent operation. If the filter criteria in <code>WHERE</code> clause does not
 * match any data, the read will return an empty result set.
 *
 * <p>A single <code>SELECT</code> statement response can return up to the maximum number of items (if using the Limit
 * parameter) or a maximum of 1 MB of data (and then apply any filtering to the results using <code>WHERE</code>
 * clause). If <code>LastEvaluatedKey</code> is present in the response, you need to paginate the result set. If <code>
 * NextToken</code> is present, you need to paginate the result set and include <code>NextToken</code>.
 */
@SmithyGenerated
public final class ExecuteStatement implements ApiOperation<ExecuteStatementInput, ExecuteStatementOutput> {

    private static final ExecuteStatement $INSTANCE = new ExecuteStatement();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#ExecuteStatement"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConditionalCheckFailedException.$ID, ConditionalCheckFailedException.class, ConditionalCheckFailedException::builder)
        .putType(DuplicateItemException.$ID, DuplicateItemException.class, DuplicateItemException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(ItemCollectionSizeLimitExceededException.$ID, ItemCollectionSizeLimitExceededException.class, ItemCollectionSizeLimitExceededException::builder)
        .putType(ProvisionedThroughputExceededException.$ID, ProvisionedThroughputExceededException.class, ProvisionedThroughputExceededException::builder)
        .putType(RequestLimitExceeded.$ID, RequestLimitExceeded.class, RequestLimitExceeded::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .putType(ThrottlingException.$ID, ThrottlingException.class, ThrottlingException::builder)
        .putType(TransactionConflictException.$ID, TransactionConflictException.class, TransactionConflictException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ExecuteStatement instance() {
        return $INSTANCE;
    }

    private ExecuteStatement() {}

    @Override
    public ShapeBuilder<ExecuteStatementInput> inputBuilder() {
        return ExecuteStatementInput.builder();
    }

    @Override
    public ShapeBuilder<ExecuteStatementOutput> outputBuilder() {
        return ExecuteStatementOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ExecuteStatementInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ExecuteStatementOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConditionalCheckFailedException.$SCHEMA, DuplicateItemException.$SCHEMA, InternalServerError.$SCHEMA, ItemCollectionSizeLimitExceededException.$SCHEMA, ProvisionedThroughputExceededException.$SCHEMA, RequestLimitExceeded.$SCHEMA, ResourceNotFoundException.$SCHEMA, ThrottlingException.$SCHEMA, TransactionConflictException.$SCHEMA);
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
