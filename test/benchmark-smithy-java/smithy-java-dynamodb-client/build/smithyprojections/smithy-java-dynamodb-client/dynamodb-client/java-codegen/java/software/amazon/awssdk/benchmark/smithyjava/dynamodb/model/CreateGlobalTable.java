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
 * Creates a global table from an existing table. A global table creates a replication relationship between two or more
 * DynamoDB tables with the same table name in the provided Regions.
 *
 * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
 * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
 * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
 *
 * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To update
 * existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading global
 * tables</a>.
 *
 * <p>If you want to add a new replica table to a global table, each of the following conditions must be true:
 *
 * <ul>
 *   <li>
 *     The table must have the same primary key as all of the other replicas.
 *   </li>
 *   <li>
 *     The table must have the same name as all of the other replicas.
 *   </li>
 *   <li>
 *     The table must have DynamoDB Streams enabled, with the stream containing both the new and the old images of
 *     the item.
 *   </li>
 *   <li>
 *     None of the replica tables in the global table can contain any data.
 *   </li>
 * </ul>
 *
 * <p> If global secondary indexes are specified, then the following conditions must also be met:
 *
 * <ul>
 *   <li>
 *      The global secondary indexes must have the same name.
 *   </li>
 *   <li>
 *      The global secondary indexes must have the same hash key and sort key (if present).
 *   </li>
 * </ul>
 *
 * <p> If local secondary indexes are specified, then the following conditions must also be met:
 *
 * <ul>
 *   <li>
 *      The local secondary indexes must have the same name.
 *   </li>
 *   <li>
 *      The local secondary indexes must have the same hash key and sort key (if present).
 *   </li>
 * </ul>
 *
 * <p> Write capacity settings should be set consistently across your replica tables and secondary indexes. DynamoDB
 * strongly recommends enabling auto scaling to manage the write capacity settings for all of your global tables
 * replicas and indexes.
 *
 * <p> If you prefer to manage write capacity settings manually, you should provision equal replicated write capacity
 * units to your replica tables. You should also provision equal replicated write capacity units to matching secondary
 * indexes across your global table.
 */
@SmithyGenerated
public final class CreateGlobalTable implements ApiOperation<CreateGlobalTableInput, CreateGlobalTableOutput> {

    private static final CreateGlobalTable $INSTANCE = new CreateGlobalTable();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#CreateGlobalTable"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(GlobalTableAlreadyExistsException.$ID, GlobalTableAlreadyExistsException.class, GlobalTableAlreadyExistsException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(TableNotFoundException.$ID, TableNotFoundException.class, TableNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static CreateGlobalTable instance() {
        return $INSTANCE;
    }

    private CreateGlobalTable() {}

    @Override
    public ShapeBuilder<CreateGlobalTableInput> inputBuilder() {
        return CreateGlobalTableInput.builder();
    }

    @Override
    public ShapeBuilder<CreateGlobalTableOutput> outputBuilder() {
        return CreateGlobalTableOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return CreateGlobalTableInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return CreateGlobalTableOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(GlobalTableAlreadyExistsException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, TableNotFoundException.$SCHEMA);
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
