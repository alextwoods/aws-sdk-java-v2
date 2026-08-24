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
 * Updates settings for a global table.
 *
 * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
 * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
 * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
 *
 * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To update
 * existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading global
 * tables</a>.
 */
@SmithyGenerated
public final class UpdateGlobalTableSettings implements ApiOperation<UpdateGlobalTableSettingsInput, UpdateGlobalTableSettingsOutput> {

    private static final UpdateGlobalTableSettings $INSTANCE = new UpdateGlobalTableSettings();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#UpdateGlobalTableSettings"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(GlobalTableNotFoundException.$ID, GlobalTableNotFoundException.class, GlobalTableNotFoundException::builder)
        .putType(IndexNotFoundException.$ID, IndexNotFoundException.class, IndexNotFoundException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(ReplicaNotFoundException.$ID, ReplicaNotFoundException.class, ReplicaNotFoundException::builder)
        .putType(ResourceInUseException.$ID, ResourceInUseException.class, ResourceInUseException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static UpdateGlobalTableSettings instance() {
        return $INSTANCE;
    }

    private UpdateGlobalTableSettings() {}

    @Override
    public ShapeBuilder<UpdateGlobalTableSettingsInput> inputBuilder() {
        return UpdateGlobalTableSettingsInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateGlobalTableSettingsOutput> outputBuilder() {
        return UpdateGlobalTableSettingsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateGlobalTableSettingsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateGlobalTableSettingsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(GlobalTableNotFoundException.$SCHEMA, IndexNotFoundException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, ReplicaNotFoundException.$SCHEMA, ResourceInUseException.$SCHEMA);
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
