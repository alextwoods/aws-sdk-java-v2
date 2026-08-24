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
 * The <code>CreateTable</code> operation adds a new table to your account. In an Amazon Web Services account, table
 * names must be unique within each Region. That is, you can have two tables with same name if you create the tables in
 * different Regions.
 *
 * <p><code>CreateTable</code> is an asynchronous operation. Upon receiving a <code>CreateTable</code> request, DynamoDB
 * immediately returns a response with a <code>TableStatus</code> of <code>CREATING</code>. After the table is created,
 * DynamoDB sets the <code>TableStatus</code> to <code>ACTIVE</code>. You can perform read and write operations only on
 * an <code>ACTIVE</code> table.
 *
 * <p>You can optionally define secondary indexes on the new table, as part of the <code>CreateTable</code> operation.
 * If you want to create multiple tables with secondary indexes on them, you must create the tables sequentially. Only
 * one table with secondary indexes can be in the <code>CREATING</code> state at any given time.
 *
 * <p>You can use the <code>DescribeTable</code> action to check the table status.
 */
@SmithyGenerated
public final class CreateTable implements ApiOperation<CreateTableInput, CreateTableOutput> {

    private static final CreateTable $INSTANCE = new CreateTable();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#CreateTable"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(ResourceInUseException.$ID, ResourceInUseException.class, ResourceInUseException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static CreateTable instance() {
        return $INSTANCE;
    }

    private CreateTable() {}

    @Override
    public ShapeBuilder<CreateTableInput> inputBuilder() {
        return CreateTableInput.builder();
    }

    @Override
    public ShapeBuilder<CreateTableOutput> outputBuilder() {
        return CreateTableOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return CreateTableInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return CreateTableOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, ResourceInUseException.$SCHEMA);
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
