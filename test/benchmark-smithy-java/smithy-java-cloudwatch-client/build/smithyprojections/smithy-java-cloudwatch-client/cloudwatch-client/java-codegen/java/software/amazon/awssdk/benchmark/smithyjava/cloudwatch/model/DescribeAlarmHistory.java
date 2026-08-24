package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Retrieves the history for the specified alarm. You can filter the results by date range or item type. If an alarm
 * name is not specified, the histories for either all metric alarms or all composite alarms are returned.
 *
 * <p>CloudWatch retains the history of an alarm even if you delete the alarm.
 *
 * <p>To use this operation and return information about a composite alarm, you must be signed on with the <code>
 * cloudwatch:DescribeAlarmHistory</code> permission that is scoped to <code>&#42;</code>. You can't return information
 * about composite alarms if your <code>cloudwatch:DescribeAlarmHistory</code> permission has a narrower scope.
 */
@SmithyGenerated
public final class DescribeAlarmHistory implements ApiOperation<DescribeAlarmHistoryInput, DescribeAlarmHistoryOutput> {

    private static final DescribeAlarmHistory $INSTANCE = new DescribeAlarmHistory();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmHistory"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").items("AlarmHistoryItems").pageSize("MaxRecords").build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidNextToken.$ID, InvalidNextToken.class, InvalidNextToken::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeAlarmHistory instance() {
        return $INSTANCE;
    }

    private DescribeAlarmHistory() {}

    @Override
    public ShapeBuilder<DescribeAlarmHistoryInput> inputBuilder() {
        return DescribeAlarmHistoryInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeAlarmHistoryOutput> outputBuilder() {
        return DescribeAlarmHistoryOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeAlarmHistoryInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeAlarmHistoryOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidNextToken.$SCHEMA);
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
