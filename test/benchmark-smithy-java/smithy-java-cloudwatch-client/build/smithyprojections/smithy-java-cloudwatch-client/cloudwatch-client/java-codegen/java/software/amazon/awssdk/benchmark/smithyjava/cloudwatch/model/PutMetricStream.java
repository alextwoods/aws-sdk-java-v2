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
 * Creates or updates a metric stream. Metric streams can automatically stream CloudWatch metrics to Amazon Web Services
 * destinations, including Amazon S3, and to many third-party solutions.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Metric-Streams.html"> Using Metric Streams</a>.
 *
 * <p>To create a metric stream, you must be signed in to an account that has the <code>iam:PassRole</code> permission
 * and either the <code>CloudWatchFullAccess</code> policy or the <code>cloudwatch:PutMetricStream</code> permission.
 *
 * <p>When you create or update a metric stream, you choose one of the following:
 *
 * <ul>
 *   <li>
 *     Stream metrics from all metric namespaces in the account.
 *   </li>
 *   <li>
 *     Stream metrics from all metric namespaces in the account, except for the namespaces that you list in <code>
 *     ExcludeFilters</code>.
 *   </li>
 *   <li>
 *     Stream metrics from only the metric namespaces that you list in <code>IncludeFilters</code>.
 *   </li>
 * </ul>
 *
 * <p>By default, a metric stream always sends the <code>MAX</code>, <code>MIN</code>, <code>SUM</code>, and <code>
 * SAMPLECOUNT</code> statistics for each metric that is streamed. You can use the <code>StatisticsConfigurations</code>
 * parameter to have the metric stream send additional statistics in the stream. Streaming additional statistics incurs
 * additional costs. For more information, see <a href="https://aws.amazon.com/cloudwatch/pricing/">Amazon CloudWatch Pricing</a>.
 *
 * <p>When you use <code>PutMetricStream</code> to create a new metric stream, the stream is created in the <code>
 * running</code> state. If you use it to update an existing stream, the state of the stream is not changed.
 *
 * <p>If you are using CloudWatch cross-account observability and you create a metric stream in a monitoring account,
 * you can choose whether to include metrics from source accounts in the stream. For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Unified-Cross-Account.html">CloudWatch
 * cross-account observability</a>.
 */
@SmithyGenerated
public final class PutMetricStream implements ApiOperation<PutMetricStreamInput, PutMetricStreamOutput> {

    private static final PutMetricStream $INSTANCE = new PutMetricStream();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutMetricStream"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConcurrentModificationException.$ID, ConcurrentModificationException.class, ConcurrentModificationException::builder)
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
    public static PutMetricStream instance() {
        return $INSTANCE;
    }

    private PutMetricStream() {}

    @Override
    public ShapeBuilder<PutMetricStreamInput> inputBuilder() {
        return PutMetricStreamInput.builder();
    }

    @Override
    public ShapeBuilder<PutMetricStreamOutput> outputBuilder() {
        return PutMetricStreamOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutMetricStreamInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutMetricStreamOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConcurrentModificationException.$SCHEMA, InternalServiceFault.$SCHEMA, InvalidParameterCombinationException.$SCHEMA, InvalidParameterValueException.$SCHEMA, MissingRequiredParameterException.$SCHEMA);
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
