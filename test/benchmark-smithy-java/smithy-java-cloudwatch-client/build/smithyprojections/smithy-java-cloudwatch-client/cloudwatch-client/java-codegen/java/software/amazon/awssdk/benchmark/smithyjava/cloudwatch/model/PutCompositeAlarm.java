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
 * Creates or updates a <i>composite alarm</i>. When you create a composite alarm, you specify a rule expression for the
 * alarm that takes into account the alarm states of other alarms that you have created. The composite alarm goes into
 * ALARM state only if all conditions of the rule are met.
 *
 * <p>The alarms specified in a composite alarm's rule expression can include metric alarms and other composite alarms.
 * The rule expression of a composite alarm can include as many as 100 underlying alarms. Any single alarm can be
 * included in the rule expressions of as many as 150 composite alarms.
 *
 * <p>Using composite alarms can reduce alarm noise. You can create multiple metric alarms, and also create a composite
 * alarm and set up alerts only for the composite alarm. For example, you could create a composite alarm that goes into
 * ALARM state only when more than one of the underlying metric alarms are in ALARM state.
 *
 * <p>Composite alarms can take the following actions:
 *
 * <ul>
 *   <li>
 *     Notify Amazon SNS topics.
 *   </li>
 *   <li>
 *     Invoke Lambda functions.
 *   </li>
 *   <li>
 *     Create OpsItems in Systems Manager Ops Center.
 *   </li>
 *   <li>
 *     Create incidents in Systems Manager Incident Manager.
 *   </li>
 * </ul>
 *
 * <p>It is possible to create a loop or cycle of composite alarms, where composite alarm A depends on composite alarm
 * B, and composite alarm B also depends on composite alarm A. In this scenario, you can't delete any composite alarm
 * that is part of the cycle because there is always still a composite alarm that depends on that alarm that you want to
 * delete.
 *
 * <p>To get out of such a situation, you must break the cycle by changing the rule of one of the composite alarms in
 * the cycle to remove a dependency that creates the cycle. The simplest change to make to break a cycle is to change
 * the <code>AlarmRule</code> of one of the alarms to <code>false</code>.
 *
 * <p>Additionally, the evaluation of composite alarms stops if CloudWatch detects a cycle in the evaluation path.
 *
 * <p>When this operation creates an alarm, the alarm state is immediately set to <code>INSUFFICIENT_DATA</code>. The
 * alarm is then evaluated and its state is set appropriately. Any actions associated with the new state are then
 * executed. For a composite alarm, this initial time after creation is the only time that the alarm can be in <code>
 * INSUFFICIENT_DATA</code> state.
 *
 * <p>When you update an existing alarm, its state is left unchanged, but the update completely overwrites the previous
 * configuration of the alarm.
 *
 * <p>To use this operation, you must be signed on with the <code>cloudwatch:PutCompositeAlarm</code> permission that is
 * scoped to <code>&#42;</code>. You can't create a composite alarms if your <code>cloudwatch:PutCompositeAlarm</code>
 * permission has a narrower scope.
 *
 * <p>If you are an IAM user, you must have <code>iam:CreateServiceLinkedRole</code> to create a composite alarm that
 * has Systems Manager OpsItem actions.
 */
@SmithyGenerated
public final class PutCompositeAlarm implements ApiOperation<PutCompositeAlarmInput, PutCompositeAlarmOutput> {

    private static final PutCompositeAlarm $INSTANCE = new PutCompositeAlarm();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutCompositeAlarm"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(LimitExceededFault.$ID, LimitExceededFault.class, LimitExceededFault::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutCompositeAlarm instance() {
        return $INSTANCE;
    }

    private PutCompositeAlarm() {}

    @Override
    public ShapeBuilder<PutCompositeAlarmInput> inputBuilder() {
        return PutCompositeAlarmInput.builder();
    }

    @Override
    public ShapeBuilder<PutCompositeAlarmOutput> outputBuilder() {
        return PutCompositeAlarmOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutCompositeAlarmInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutCompositeAlarmOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(LimitExceededFault.$SCHEMA);
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
