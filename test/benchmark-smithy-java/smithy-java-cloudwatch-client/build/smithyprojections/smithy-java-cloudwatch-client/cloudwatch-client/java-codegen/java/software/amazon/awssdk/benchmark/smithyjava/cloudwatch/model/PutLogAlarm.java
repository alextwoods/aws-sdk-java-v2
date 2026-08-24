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
 * Creates or updates a log alarm. A log alarm evaluates the results of a CloudWatch Logs scheduled query against the
 * configured threshold and comparison operator to determine its state.
 *
 * <p>When you create a log alarm, the operation creates a service-managed CloudWatch Logs scheduled query that runs the
 * query string you provide on the schedule you configure. Each scheduled query execution returns one or more aggregated
 * values determined by the <code>AggregationExpression</code>, and each aggregated value is compared against the alarm <code>
 * Threshold</code> to determine the alarm state. The alarm uses M-out-of-N evaluation: if <code>QueryResultsToAlarm</code>
 * out of the most recent <code>QueryResultsToEvaluate</code> query results breach the threshold, the alarm transitions
 * to <code>ALARM</code>.
 *
 * <p>Log alarms support the alarm states (<code>OK</code>, <code>ALARM</code>, <code>INSUFFICIENT_DATA</code>).
 * Configure transition actions using <code>OKActions</code>, <code>AlarmActions</code>, and <code>
 * InsufficientDataActions</code>.
 *
 * <p>If you call this operation with the name of an existing log alarm, the operation replaces the previous
 * configuration of that alarm.
 *
 * <p><b>Permissions</b>
 *
 * <p>To create or update a log alarm, you must have the <code>cloudwatch:PutLogAlarm</code> permission. The IAM role
 * specified in <code>ScheduledQueryRoleARN</code> must grant the CloudWatch Alarms service permission to execute
 * scheduled queries on the specified log groups. If you set <code>ActionLogLineCount</code>, the role specified in <code>
 * ActionLogLineRoleArn</code> must grant permission to retrieve log events for inclusion in alarm notifications.
 */
@SmithyGenerated
public final class PutLogAlarm implements ApiOperation<PutLogAlarmInput, PutLogAlarmOutput> {

    private static final PutLogAlarm $INSTANCE = new PutLogAlarm();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutLogAlarm"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(LimitExceededFault.$ID, LimitExceededFault.class, LimitExceededFault::builder)
        .putType(ResourceConflict.$ID, ResourceConflict.class, ResourceConflict::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutLogAlarm instance() {
        return $INSTANCE;
    }

    private PutLogAlarm() {}

    @Override
    public ShapeBuilder<PutLogAlarmInput> inputBuilder() {
        return PutLogAlarmInput.builder();
    }

    @Override
    public ShapeBuilder<PutLogAlarmOutput> outputBuilder() {
        return PutLogAlarmOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutLogAlarmInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutLogAlarmOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(LimitExceededFault.$SCHEMA, ResourceConflict.$SCHEMA);
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
