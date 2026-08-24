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
 * Retrieves the alarms for the specified metric. To filter the results, specify a statistic, period, or unit.
 *
 * <p>This operation retrieves only standard alarms that are based on the specified metric. It does not return alarms
 * based on math expressions that use the specified metric, or composite alarms that use the specified metric.
 */
@SmithyGenerated
public final class DescribeAlarmsForMetric implements ApiOperation<DescribeAlarmsForMetricInput, DescribeAlarmsForMetricOutput> {

    private static final DescribeAlarmsForMetric $INSTANCE = new DescribeAlarmsForMetric();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmsForMetric"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeAlarmsForMetric instance() {
        return $INSTANCE;
    }

    private DescribeAlarmsForMetric() {}

    @Override
    public ShapeBuilder<DescribeAlarmsForMetricInput> inputBuilder() {
        return DescribeAlarmsForMetricInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeAlarmsForMetricOutput> outputBuilder() {
        return DescribeAlarmsForMetricOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeAlarmsForMetricInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeAlarmsForMetricOutput.$SCHEMA;
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
