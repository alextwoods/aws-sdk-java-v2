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
 * Enables the actions for the specified alarms.
 */
@SmithyGenerated
public final class EnableAlarmActions implements ApiOperation<EnableAlarmActionsInput, EnableAlarmActionsOutput> {

    private static final EnableAlarmActions $INSTANCE = new EnableAlarmActions();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#EnableAlarmActions"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static EnableAlarmActions instance() {
        return $INSTANCE;
    }

    private EnableAlarmActions() {}

    @Override
    public ShapeBuilder<EnableAlarmActionsInput> inputBuilder() {
        return EnableAlarmActionsInput.builder();
    }

    @Override
    public ShapeBuilder<EnableAlarmActionsOutput> outputBuilder() {
        return EnableAlarmActionsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return EnableAlarmActionsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return EnableAlarmActionsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of();
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
