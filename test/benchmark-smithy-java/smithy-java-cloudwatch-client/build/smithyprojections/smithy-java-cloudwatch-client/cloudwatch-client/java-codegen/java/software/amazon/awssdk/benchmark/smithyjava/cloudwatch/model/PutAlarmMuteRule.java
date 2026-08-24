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
 * Creates or updates an alarm mute rule.
 *
 * <p>Alarm mute rules automatically mute alarm actions during predefined time windows. When a mute rule is active,
 * targeted alarms continue to evaluate metrics and transition between states, but their configured actions (such as
 * Amazon SNS notifications or Auto Scaling actions) are muted.
 *
 * <p>You can create mute rules with recurring schedules using <code>cron</code> expressions or one-time mute windows
 * using <code>at</code> expressions. Each mute rule can target up to 100 specific alarms by name.
 *
 * <p>If you specify a rule name that already exists, this operation updates the existing rule with the new
 * configuration.
 *
 * <p><b>Permissions</b>
 *
 * <p>To create or update a mute rule, you must have the <code>cloudwatch:PutAlarmMuteRule</code> permission on two
 * types of resources: the alarm mute rule resource itself, and each alarm that the rule targets.
 *
 * <p>For example, If you want to allow a user to create mute rules that target only specific alarms named
 * "WebServerCPUAlarm" and "DatabaseConnectionAlarm", you would create an IAM policy with one statement granting <code>
 * cloudwatch:PutAlarmMuteRule</code> on the alarm mute rule resource (<code>
 * arn:aws:cloudwatch:[REGION]:123456789012:alarm-mute-rule:&#42;</code>), and another statement granting <code>
 * cloudwatch:PutAlarmMuteRule</code> on the targeted alarm resources (<code>
 * arn:aws:cloudwatch:[REGION]:123456789012:alarm:WebServerCPUAlarm</code> and <code>
 * arn:aws:cloudwatch:[REGION]:123456789012:alarm:DatabaseConnectionAlarm</code>).
 *
 * <p>You can also use IAM policy conditions to allow targeting alarms based on resource tags. For example, you can
 * restrict users to create/update mute rules to only target alarms that have a specific tag key-value pair, such as <code>
 * Team=TeamA</code>.
 */
@SmithyGenerated
public final class PutAlarmMuteRule implements ApiOperation<PutAlarmMuteRuleInput, PutAlarmMuteRuleOutput> {

    private static final PutAlarmMuteRule $INSTANCE = new PutAlarmMuteRule();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutAlarmMuteRule"));

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
    public static PutAlarmMuteRule instance() {
        return $INSTANCE;
    }

    private PutAlarmMuteRule() {}

    @Override
    public ShapeBuilder<PutAlarmMuteRuleInput> inputBuilder() {
        return PutAlarmMuteRuleInput.builder();
    }

    @Override
    public ShapeBuilder<PutAlarmMuteRuleOutput> outputBuilder() {
        return PutAlarmMuteRuleOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutAlarmMuteRuleInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutAlarmMuteRuleOutput.$SCHEMA;
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
