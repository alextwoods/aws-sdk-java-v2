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
 * You can use the <code>GetMetricData</code> API to retrieve CloudWatch metric values. The operation can also include a
 * CloudWatch Metrics Insights query, and one or more metric math functions.
 *
 * <p>A <code>GetMetricData</code> operation that does not include a query can retrieve as many as 500 different metrics
 * in a single request, with a total of as many as 100,800 data points. You can also optionally perform metric math
 * expressions on the values of the returned statistics, to create new time series that represent new insights into your
 * data. For example, using Lambda metrics, you could divide the Errors metric by the Invocations metric to get an error
 * rate time series. For more information about metric math expressions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/using-metric-math.html#metric-math-syntax">Metric Math Syntax and Functions</a> in
 * the <i>Amazon CloudWatch User Guide</i>.
 *
 * <p>If you include a Metrics Insights query, each <code>GetMetricData</code> operation can include only one query. But
 * the same <code>GetMetricData</code> operation can also retrieve other metrics. Metrics Insights queries can query
 * only the most recent three hours of metric data. For more information about Metrics Insights, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/query_with_cloudwatch-metrics-insights.html">Query your
 * metrics with CloudWatch Metrics Insights</a>.
 *
 * <p>Calls to the <code>GetMetricData</code> API have a different pricing structure than calls to <code>
 * GetMetricStatistics</code>. For more information about pricing, see <a href="https://aws.amazon.com/cloudwatch/pricing/">Amazon CloudWatch Pricing</a>.
 *
 * <p>Amazon CloudWatch retains metric data as follows:
 *
 * <ul>
 *   <li>
 *     Data points with a period of less than 60 seconds are available for 3 hours. These data points are
 *     high-resolution metrics and are available only for custom metrics that have been defined with a <code>
 *     StorageResolution</code> of 1.
 *   </li>
 *   <li>
 *     Data points with a period of 60 seconds (1-minute) are available for 15 days.
 *   </li>
 *   <li>
 *     Data points with a period of 300 seconds (5-minute) are available for 63 days.
 *   </li>
 *   <li>
 *     Data points with a period of 3600 seconds (1 hour) are available for 455 days (15 months).
 *   </li>
 * </ul>
 *
 * <p>Data points that are initially published with a shorter period are aggregated together for long-term storage. For
 * example, if you collect data using a period of 1 minute, the data remains available for 15 days with 1-minute
 * resolution. After 15 days, this data is still available, but is aggregated and retrievable only with a resolution of
 * 5 minutes. After 63 days, the data is further aggregated and is available with a resolution of 1 hour.
 *
 * <p>If you omit <code>Unit</code> in your request, all data that was collected with any unit is returned, along with
 * the corresponding units that were specified when the data was reported to CloudWatch. If you specify a unit, the
 * operation returns only data that was collected with that unit specified. If you specify a unit that does not match
 * the data collected, the results of the operation are null. CloudWatch does not perform unit conversions.
 *
 * <p><b>Using Metrics Insights queries with metric math</b>
 *
 * <p>You can't mix a Metric Insights query and metric math syntax in the same expression, but you can reference results
 * from a Metrics Insights query within other Metric math expressions. A Metrics Insights query without a <b>GROUP BY</b>
 * clause returns a single time-series (TS), and can be used as input for a metric math expression that expects a single
 * time series. A Metrics Insights query with a <b>GROUP BY</b> clause returns an array of time-series (TS[]), and can
 * be used as input for a metric math expression that expects an array of time series.
 */
@SmithyGenerated
public final class GetMetricData implements ApiOperation<GetMetricDataInput, GetMetricDataOutput> {

    private static final GetMetricData $INSTANCE = new GetMetricData();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetMetricData"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").pageSize("MaxDatapoints").build());

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
    public static GetMetricData instance() {
        return $INSTANCE;
    }

    private GetMetricData() {}

    @Override
    public ShapeBuilder<GetMetricDataInput> inputBuilder() {
        return GetMetricDataInput.builder();
    }

    @Override
    public ShapeBuilder<GetMetricDataOutput> outputBuilder() {
        return GetMetricDataOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetMetricDataInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetMetricDataOutput.$SCHEMA;
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
