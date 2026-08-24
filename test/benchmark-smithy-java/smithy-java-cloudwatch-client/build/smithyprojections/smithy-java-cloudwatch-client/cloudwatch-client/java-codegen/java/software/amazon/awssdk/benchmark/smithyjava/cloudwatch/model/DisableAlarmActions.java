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
 * Disables the actions for the specified alarms. When an alarm's actions are disabled, the alarm actions do not execute
 * when the alarm state changes.
 */
@SmithyGenerated
public final class DisableAlarmActions implements ApiOperation<DisableAlarmActionsInput, DisableAlarmActionsOutput> {

    private static final DisableAlarmActions $INSTANCE = new DisableAlarmActions();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DisableAlarmActions"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DisableAlarmActions instance() {
        return $INSTANCE;
    }

    private DisableAlarmActions() {}

    @Override
    public ShapeBuilder<DisableAlarmActionsInput> inputBuilder() {
        return DisableAlarmActionsInput.builder();
    }

    @Override
    public ShapeBuilder<DisableAlarmActionsOutput> outputBuilder() {
        return DisableAlarmActionsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DisableAlarmActionsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DisableAlarmActionsOutput.$SCHEMA;
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
