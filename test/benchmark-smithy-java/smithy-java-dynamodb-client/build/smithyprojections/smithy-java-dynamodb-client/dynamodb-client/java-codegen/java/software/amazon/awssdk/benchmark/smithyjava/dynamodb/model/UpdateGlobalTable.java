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
 * Adds or removes replicas in the specified global table. The global table must already exist to be able to use this
 * operation. Any replica to be added must be empty, have the same name as the global table, have the same key schema,
 * have DynamoDB Streams enabled, and have the same provisioned and maximum write capacity units.
 *
 * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
 * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
 * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
 *
 * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To update
 * existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading global
 * tables</a>.
 *
 * <p> If you are using global tables <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Version 2019.11.21</a> (Current) you can use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_UpdateTable.html">UpdateTable</a> instead.
 *
 * <p> Although you can use <code>UpdateGlobalTable</code> to add replicas and remove replicas in a single request, for
 * simplicity we recommend that you issue separate requests for adding or removing replicas.
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
 *   <li>
 *      The global secondary indexes must have the same provisioned and maximum write capacity units.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class UpdateGlobalTable implements ApiOperation<UpdateGlobalTableInput, UpdateGlobalTableOutput> {

    private static final UpdateGlobalTable $INSTANCE = new UpdateGlobalTable();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#UpdateGlobalTable"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(GlobalTableNotFoundException.$ID, GlobalTableNotFoundException.class, GlobalTableNotFoundException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(ReplicaAlreadyExistsException.$ID, ReplicaAlreadyExistsException.class, ReplicaAlreadyExistsException::builder)
        .putType(ReplicaNotFoundException.$ID, ReplicaNotFoundException.class, ReplicaNotFoundException::builder)
        .putType(TableNotFoundException.$ID, TableNotFoundException.class, TableNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UpdateGlobalTable instance() {
        return $INSTANCE;
    }

    private UpdateGlobalTable() {}

    @Override
    public ShapeBuilder<UpdateGlobalTableInput> inputBuilder() {
        return UpdateGlobalTableInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateGlobalTableOutput> outputBuilder() {
        return UpdateGlobalTableOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateGlobalTableInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateGlobalTableOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(GlobalTableNotFoundException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, ReplicaAlreadyExistsException.$SCHEMA, ReplicaNotFoundException.$SCHEMA, TableNotFoundException.$SCHEMA);
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
