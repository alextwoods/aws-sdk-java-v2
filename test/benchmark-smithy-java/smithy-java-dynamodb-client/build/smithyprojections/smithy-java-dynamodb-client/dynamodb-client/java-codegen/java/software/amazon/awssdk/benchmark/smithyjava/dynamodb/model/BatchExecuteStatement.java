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
 * This operation allows you to perform batch reads or writes on data stored in DynamoDB, using PartiQL. Each read
 * statement in a <code>BatchExecuteStatement</code> must specify an equality condition on all key attributes. This
 * enforces that each <code>SELECT</code> statement in a batch returns at most a single item. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.multiplestatements.batching.html">
 * Running batch operations with PartiQL for DynamoDB </a>.
 *
 * <p>The entire batch must consist of either read statements or write statements, you cannot mix both in one batch.
 *
 * <p>A HTTP 200 response does not mean that all statements in the BatchExecuteStatement succeeded. Error details for
 * individual statements can be found under the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_BatchStatementResponse.html#DDB-Type-BatchStatementResponse-Error">Error</a> field of the <code>BatchStatementResponse</code> for each
 * statement.
 */
@SmithyGenerated
public final class BatchExecuteStatement implements ApiOperation<BatchExecuteStatementInput, BatchExecuteStatementOutput> {

    private static final BatchExecuteStatement $INSTANCE = new BatchExecuteStatement();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#BatchExecuteStatement"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(RequestLimitExceeded.$ID, RequestLimitExceeded.class, RequestLimitExceeded::builder)
        .putType(ThrottlingException.$ID, ThrottlingException.class, ThrottlingException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static BatchExecuteStatement instance() {
        return $INSTANCE;
    }

    private BatchExecuteStatement() {}

    @Override
    public ShapeBuilder<BatchExecuteStatementInput> inputBuilder() {
        return BatchExecuteStatementInput.builder();
    }

    @Override
    public ShapeBuilder<BatchExecuteStatementOutput> outputBuilder() {
        return BatchExecuteStatementOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return BatchExecuteStatementInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return BatchExecuteStatementOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, RequestLimitExceeded.$SCHEMA, ThrottlingException.$SCHEMA);
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
