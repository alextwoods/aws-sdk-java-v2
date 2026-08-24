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
 * Gets statistics for the specified metric.
 *
 * <p>The maximum number of data points returned from a single call is 1,440. If you request more than 1,440 data
 * points, CloudWatch returns an error. To reduce the number of data points, you can narrow the specified time range and
 * make multiple requests across adjacent time ranges, or you can increase the specified period. Data points are not
 * returned in chronological order.
 *
 * <p>CloudWatch aggregates data points based on the length of the period that you specify. For example, if you request
 * statistics with a one-hour period, CloudWatch aggregates all data points with time stamps that fall within each
 * one-hour period. Therefore, the number of values aggregated by CloudWatch is larger than the number of data points
 * returned.
 *
 * <p>CloudWatch needs raw data points to calculate percentile statistics. If you publish data using a statistic set
 * instead, you can only retrieve percentile statistics for this data if one of the following conditions is true:
 *
 * <ul>
 *   <li>
 *     The SampleCount value of the statistic set is 1.
 *   </li>
 *   <li>
 *     The Min and the Max values of the statistic set are equal.
 *   </li>
 * </ul>
 *
 * <p>Percentile statistics are not available for metrics when any of the metric values are negative numbers.
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
 * <p>CloudWatch started retaining 5-minute and 1-hour metric data as of July 9, 2016.
 *
 * <p>For information about metrics and dimensions supported by Amazon Web Services services, see the <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CW_Support_For_AWS.html">Amazon
 * CloudWatch Metrics and Dimensions Reference</a> in the <i>Amazon CloudWatch User Guide</i>.
 */
@SmithyGenerated
public final class GetMetricStatistics implements ApiOperation<GetMetricStatisticsInput, GetMetricStatisticsOutput> {

    private static final GetMetricStatistics $INSTANCE = new GetMetricStatistics();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetMetricStatistics"));

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
    public static GetMetricStatistics instance() {
        return $INSTANCE;
    }

    private GetMetricStatistics() {}

    @Override
    public ShapeBuilder<GetMetricStatisticsInput> inputBuilder() {
        return GetMetricStatisticsInput.builder();
    }

    @Override
    public ShapeBuilder<GetMetricStatisticsOutput> outputBuilder() {
        return GetMetricStatisticsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetMetricStatisticsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetMetricStatisticsOutput.$SCHEMA;
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
