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
 * Retrieves the specified alarms. You can filter the results by specifying a prefix for the alarm name, the alarm
 * state, or a prefix for any action.
 *
 * <p>To use this operation and return information about composite alarms, you must be signed on with the <code>
 * cloudwatch:DescribeAlarms</code> permission that is scoped to <code>&#42;</code>. You can't return information about
 * composite alarms if your <code>cloudwatch:DescribeAlarms</code> permission has a narrower scope.
 */
@SmithyGenerated
public final class DescribeAlarms implements ApiOperation<DescribeAlarmsInput, DescribeAlarmsOutput> {

    private static final DescribeAlarms $INSTANCE = new DescribeAlarms();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarms"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").pageSize("MaxRecords").build());

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
    public static DescribeAlarms instance() {
        return $INSTANCE;
    }

    private DescribeAlarms() {}

    @Override
    public ShapeBuilder<DescribeAlarmsInput> inputBuilder() {
        return DescribeAlarmsInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeAlarmsOutput> outputBuilder() {
        return DescribeAlarmsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeAlarmsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeAlarmsOutput.$SCHEMA;
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
