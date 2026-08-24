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
 * Creates or updates an alarm and associates it with the specified metric, metric math expression, anomaly detection
 * model, Metrics Insights query, or PromQL query. For more information about using a Metrics Insights query for an
 * alarm, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Create_Metrics_Insights_Alarm.html">Create alarms on Metrics Insights queries</a>.
 *
 * <p>Alarms based on anomaly detection models cannot have Auto Scaling actions.
 *
 * <p>When this operation creates an alarm, the alarm state is immediately set to <code>INSUFFICIENT_DATA</code>. For
 * PromQL alarms, the alarm state is instead immediately set to <code>OK</code>. The alarm is then evaluated and its
 * state is set appropriately. Any actions associated with the new state are then executed.
 *
 * <p>When you update an existing alarm, its state is left unchanged, but the update completely overwrites the previous
 * configuration of the alarm.
 *
 * <p>If you are an IAM user, you must have Amazon EC2 permissions for some alarm operations:
 *
 * <ul>
 *   <li>
 *     The <code>iam:CreateServiceLinkedRole</code> permission for all alarms with EC2 actions
 *   </li>
 *   <li>
 *     The <code>iam:CreateServiceLinkedRole</code> permissions to create an alarm with Systems Manager OpsItem or
 *     response plan actions.
 *   </li>
 * </ul>
 *
 * <p>The first time you create an alarm in the Amazon Web Services Management Console, the CLI, or by using the
 * PutMetricAlarm API, CloudWatch creates the necessary service-linked role for you. The service-linked roles are called
 * <code>AWSServiceRoleForCloudWatchEvents</code> and <code>AWSServiceRoleForCloudWatchAlarms_ActionSSM</code>. For more
 * information, see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_terms-and-concepts.html#iam-term-service-linked-role">Amazon Web Services service-linked role</a>.
 *
 * <p>Each <code>PutMetricAlarm</code> action has a maximum uncompressed payload of 120 KB.
 *
 * <p><b>Cross-account alarms</b>
 *
 * <p>You can set an alarm on metrics in the current account, or in another account. To create a cross-account alarm
 * that watches a metric in a different account, you must have completed the following pre-requisites:
 *
 * <ul>
 *   <li>
 *     The account where the metrics are located (the <i>sharing account</i>) must already have a sharing role named
 *     <b>CloudWatch-CrossAccountSharingRole</b>. If it does not already have this role, you must create it using
 *     the instructions in <b>Set up a sharing account</b> in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Cross-Account-Cross-Region.html#enable-cross-account-cross-Region"> Cross-account cross-Region CloudWatch console</a>.
 *     The policy for that role must grant access to the ID of the account where you are creating the alarm.
 *   </li>
 *   <li>
 *     The account where you are creating the alarm (the <i>monitoring account</i>) must already have a
 *     service-linked role named <b>AWSServiceRoleForCloudWatchCrossAccount</b> to allow CloudWatch to assume the
 *     sharing role in the sharing account. If it does not, you must create it following the directions in <b>Set up
 *     a monitoring account</b> in <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Cross-Account-Cross-Region.html#enable-cross-account-cross-Region"> Cross-account cross-Region CloudWatch console</a>.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class PutMetricAlarm implements ApiOperation<PutMetricAlarmInput, PutMetricAlarmOutput> {

    private static final PutMetricAlarm $INSTANCE = new PutMetricAlarm();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutMetricAlarm"));

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
    public static PutMetricAlarm instance() {
        return $INSTANCE;
    }

    private PutMetricAlarm() {}

    @Override
    public ShapeBuilder<PutMetricAlarmInput> inputBuilder() {
        return PutMetricAlarmInput.builder();
    }

    @Override
    public ShapeBuilder<PutMetricAlarmOutput> outputBuilder() {
        return PutMetricAlarmOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutMetricAlarmInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutMetricAlarmOutput.$SCHEMA;
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
