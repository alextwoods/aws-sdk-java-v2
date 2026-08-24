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
 * Retrieves details for a specific alarm mute rule.
 *
 * <p>This operation returns complete information about the mute rule, including its configuration, status, targeted
 * alarms, and metadata.
 *
 * <p>The returned status indicates the current state of the mute rule:
 *
 * <ul>
 *   <li>
 *     <b>SCHEDULED</b>: The mute rule is configured and will become active in the future
 *   </li>
 *   <li>
 *     <b>ACTIVE</b>: The mute rule is currently muting alarm actions
 *   </li>
 *   <li>
 *     <b>EXPIRED</b>: The mute rule has passed its expiration date and will no longer become active
 *   </li>
 * </ul>
 *
 * <p><b>Permissions</b>
 *
 * <p>To retrieve details for a mute rule, you need the <code>cloudwatch:GetAlarmMuteRule</code> permission on the alarm
 * mute rule resource.
 */
@SmithyGenerated
public final class GetAlarmMuteRule implements ApiOperation<GetAlarmMuteRuleInput, GetAlarmMuteRuleOutput> {

    private static final GetAlarmMuteRule $INSTANCE = new GetAlarmMuteRule();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetAlarmMuteRule"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetAlarmMuteRule instance() {
        return $INSTANCE;
    }

    private GetAlarmMuteRule() {}

    @Override
    public ShapeBuilder<GetAlarmMuteRuleInput> inputBuilder() {
        return GetAlarmMuteRuleInput.builder();
    }

    @Override
    public ShapeBuilder<GetAlarmMuteRuleOutput> outputBuilder() {
        return GetAlarmMuteRuleOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetAlarmMuteRuleInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetAlarmMuteRuleOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ResourceNotFoundException.$SCHEMA);
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
