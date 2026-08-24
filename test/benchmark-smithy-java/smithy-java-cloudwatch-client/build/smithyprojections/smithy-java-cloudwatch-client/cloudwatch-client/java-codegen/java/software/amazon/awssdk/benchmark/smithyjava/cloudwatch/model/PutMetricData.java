package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Publishes metric data to Amazon CloudWatch. CloudWatch associates the data with the specified metric. If the
 * specified metric does not exist, CloudWatch creates the metric. When CloudWatch creates a metric, it can take up to
 * fifteen minutes for the metric to appear in calls to <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_ListMetrics.html">ListMetrics</a>.
 *
 * <p>You can publish metrics with associated entity data (so that related telemetry can be found and viewed together),
 * or publish metric data by itself. To send entity data with your metrics, use the <code>EntityMetricData</code>
 * parameter. To send metrics without entity data, use the <code>MetricData</code> parameter. The <code>EntityMetricData</code>
 * structure includes <code>MetricData</code> structures for the metric data.
 *
 * <p>You can publish either individual values in the <code>Value</code> field, or arrays of values and the number of
 * times each value occurred during the period by using the <code>Values</code> and <code>Counts</code> fields in the <code>
 * MetricData</code> structure. Using the <code>Values</code> and <code>Counts</code> method enables you to publish up
 * to 150 values per metric with one <code>PutMetricData</code> request, and supports retrieving percentile statistics
 * on this data.
 *
 * <p>Each <code>PutMetricData</code> request is limited to 1 MB in size for HTTP POST requests. You can send a payload
 * compressed by gzip. Each request is also limited to no more than 1000 different metrics (across both the <code>
 * MetricData</code> and <code>EntityMetricData</code> properties).
 *
 * <p>Although the <code>Value</code> parameter accepts numbers of type <code>Double</code>, CloudWatch rejects values
 * that are either too small or too large. Values must be in the range of -2^360 to 2^360. In addition, special values
 * (for example, NaN, +Infinity, -Infinity) are not supported.
 *
 * <p>You can use up to 30 dimensions per metric to further clarify what data the metric collects. Each dimension
 * consists of a Name and Value pair. For more information about specifying dimensions, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/publishingMetrics.html">Publishing Metrics</a> in
 * the <i>Amazon CloudWatch User Guide</i>.
 *
 * <p>You specify the time stamp to be associated with each data point. You can specify time stamps that are as much as
 * two weeks before the current date, and as much as 2 hours after the current day and time.
 *
 * <p>Data points with time stamps from 24 hours ago or longer can take at least 48 hours to become available for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">
 * GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">GetMetricStatistics</a> from the time they are submitted. Data points with time stamps
 * between 3 and 24 hours ago can take as much as 2 hours to become available for <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricData.html">GetMetricData</a> or <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetMetricStatistics.html">
 * GetMetricStatistics</a>.
 *
 * <p>CloudWatch needs raw data points to calculate percentile statistics. If you publish data using a statistic set
 * instead, you can only retrieve percentile statistics for this data if one of the following conditions is true:
 *
 * <ul>
 *   <li>
 *     The <code>SampleCount</code> value of the statistic set is 1 and <code>Min</code>, <code>Max</code>, and <code>
 *     Sum</code> are all equal.
 *   </li>
 *   <li>
 *     The <code>Min</code> and <code>Max</code> are equal, and <code>Sum</code> is equal to <code>Min</code>
 *     multiplied by <code>SampleCount</code>.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class PutMetricData implements ApiOperation<PutMetricDataInput, PutMetricDataOutput> {

    private static final PutMetricData $INSTANCE = new PutMetricData();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutMetricData"),
            RequestCompressionTrait.builder().encodings(List.of("gzip")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidParameterCombinationException.$ID, InvalidParameterCombinationException.class, InvalidParameterCombinationException::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .putType(MissingRequiredParameterException.$ID, MissingRequiredParameterException.class, MissingRequiredParameterException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutMetricData instance() {
        return $INSTANCE;
    }

    private PutMetricData() {}

    @Override
    public ShapeBuilder<PutMetricDataInput> inputBuilder() {
        return PutMetricDataInput.builder();
    }

    @Override
    public ShapeBuilder<PutMetricDataOutput> outputBuilder() {
        return PutMetricDataOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutMetricDataInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutMetricDataOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServiceFault.$SCHEMA, InvalidParameterCombinationException.$SCHEMA, InvalidParameterValueException.$SCHEMA, MissingRequiredParameterException.$SCHEMA);
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
