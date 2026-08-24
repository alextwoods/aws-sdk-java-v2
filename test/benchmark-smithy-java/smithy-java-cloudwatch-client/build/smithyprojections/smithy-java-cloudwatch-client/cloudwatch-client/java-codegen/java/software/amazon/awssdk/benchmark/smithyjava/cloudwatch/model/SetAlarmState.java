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
 * Temporarily sets the state of an alarm for testing purposes. When the updated state differs from the previous value,
 * the action configured for the appropriate state is invoked. For example, if your alarm is configured to send an
 * Amazon SNS message when an alarm is triggered, temporarily changing the alarm state to <code>ALARM</code> sends an
 * SNS message.
 *
 * <p>Metric alarms returns to their actual state quickly, often within seconds. Because the metric alarm state change
 * happens quickly, it is typically only visible in the alarm's <b>History</b> tab in the Amazon CloudWatch console or
 * through <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DescribeAlarmHistory.html">DescribeAlarmHistory</a>.
 *
 * <p>If you use <code>SetAlarmState</code> on a composite alarm, the composite alarm is not guaranteed to return to its
 * actual state. It returns to its actual state only once any of its children alarms change state. It is also
 * reevaluated if you update its configuration.
 *
 * <p>If an alarm triggers EC2 Auto Scaling policies or application Auto Scaling policies, you must include information
 * in the <code>StateReasonData</code> parameter to enable the policy to take the correct action.
 */
@SmithyGenerated
public final class SetAlarmState implements ApiOperation<SetAlarmStateInput, SetAlarmStateOutput> {

    private static final SetAlarmState $INSTANCE = new SetAlarmState();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#SetAlarmState"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidFormatFault.$ID, InvalidFormatFault.class, InvalidFormatFault::builder)
        .putType(ResourceNotFound.$ID, ResourceNotFound.class, ResourceNotFound::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static SetAlarmState instance() {
        return $INSTANCE;
    }

    private SetAlarmState() {}

    @Override
    public ShapeBuilder<SetAlarmStateInput> inputBuilder() {
        return SetAlarmStateInput.builder();
    }

    @Override
    public ShapeBuilder<SetAlarmStateOutput> outputBuilder() {
        return SetAlarmStateOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return SetAlarmStateInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return SetAlarmStateOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidFormatFault.$SCHEMA, ResourceNotFound.$SCHEMA);
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
