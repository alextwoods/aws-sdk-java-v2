package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Returns an array of table names associated with the current account and endpoint. The output from
 * <code>ListTables</code> is paginated, with each page returning a maximum of 100 table names.
 *
 * <h2>Examples</h2>
 * <h3>To list tables</h3>
 *
 * <p>This example lists all of the tables associated with the current AWS account and endpoint.{@snippet :
 * var input = ListTablesInput.builder()
 *
 *                 .build();
 *
 * var result = client.listTables(input);
 * result.equals(ListTablesOutput.builder()
 *                   .tableNames(List.of(
 *                                   "Forum",
 *                                   "ProductCatalog",
 *                                   "Reply",
 *                                   "Thread"
 *                               ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class ListTables implements ApiOperation<ListTablesInput, ListTablesOutput> {

    private static final ListTables $INSTANCE = new ListTables();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#ListTables"),
            PaginatedTrait.builder().inputToken("ExclusiveStartTableName").outputToken("LastEvaluatedTableName").items("TableNames").pageSize("Limit").build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListTables instance() {
        return $INSTANCE;
    }

    private ListTables() {}

    @Override
    public ShapeBuilder<ListTablesInput> inputBuilder() {
        return ListTablesInput.builder();
    }

    @Override
    public ShapeBuilder<ListTablesOutput> outputBuilder() {
        return ListTablesOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListTablesInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListTablesOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA);
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
