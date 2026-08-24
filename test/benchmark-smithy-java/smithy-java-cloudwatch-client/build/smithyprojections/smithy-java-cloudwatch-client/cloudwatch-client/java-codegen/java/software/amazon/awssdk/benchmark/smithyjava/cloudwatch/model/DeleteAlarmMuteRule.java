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
 * Deletes a specific alarm mute rule.
 *
 * <p>When you delete a mute rule, any alarms that are currently being muted by that rule are immediately unmuted. If
 * those alarms are in an ALARM state, their configured actions will trigger.
 *
 * <p>This operation is idempotent. If you delete a mute rule that does not exist, the operation succeeds without
 * returning an error.
 *
 * <p><b>Permissions</b>
 *
 * <p>To delete a mute rule, you need the <code>cloudwatch:DeleteAlarmMuteRule</code> permission on the alarm mute rule
 * resource.
 */
@SmithyGenerated
public final class DeleteAlarmMuteRule implements ApiOperation<DeleteAlarmMuteRuleInput, DeleteAlarmMuteRuleOutput> {

    private static final DeleteAlarmMuteRule $INSTANCE = new DeleteAlarmMuteRule();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DeleteAlarmMuteRule"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteAlarmMuteRule instance() {
        return $INSTANCE;
    }

    private DeleteAlarmMuteRule() {}

    @Override
    public ShapeBuilder<DeleteAlarmMuteRuleInput> inputBuilder() {
        return DeleteAlarmMuteRuleInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteAlarmMuteRuleOutput> outputBuilder() {
        return DeleteAlarmMuteRuleOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteAlarmMuteRuleInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteAlarmMuteRuleOutput.$SCHEMA;
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
