package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Deletes all dashboards that you specify. You can specify up to 100 dashboards to delete. If there is an error during
 * this call, the operation attempts to delete as many dashboards as possible.
 */
@SmithyGenerated
public final class DeleteDashboards implements ApiOperation<DeleteDashboardsInput, DeleteDashboardsOutput> {

    private static final DeleteDashboards $INSTANCE = new DeleteDashboards();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DeleteDashboards"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConflictException.$ID, ConflictException.class, ConflictException::builder)
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteDashboards instance() {
        return $INSTANCE;
    }

    private DeleteDashboards() {}

    @Override
    public ShapeBuilder<DeleteDashboardsInput> inputBuilder() {
        return DeleteDashboardsInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteDashboardsOutput> outputBuilder() {
        return DeleteDashboardsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteDashboardsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteDashboardsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConflictException.$SCHEMA, InternalServiceFault.$SCHEMA, InvalidParameterValueException.$SCHEMA);
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
        return CloudWatchApiService.instance();
    }
    }
