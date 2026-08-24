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
 * You can use the <code>GetMetricWidgetImage</code> API to retrieve a snapshot graph of one or more Amazon CloudWatch
 * metrics as a bitmap image. You can then embed this image into your services and products, such as wiki pages,
 * reports, and documents. You could also retrieve images regularly, such as every minute, and create your own custom
 * live dashboard.
 *
 * <p>The graph you retrieve can include all CloudWatch metric graph features, including metric math and horizontal and
 * vertical annotations.
 *
 * <p>There is a limit of 20 transactions per second for this API. Each <code>GetMetricWidgetImage</code> action has the
 * following limits:
 *
 * <ul>
 *   <li>
 *     As many as 100 metrics in the graph.
 *   </li>
 *   <li>
 *     Up to 100 KB uncompressed payload.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class GetMetricWidgetImage implements ApiOperation<GetMetricWidgetImageInput, GetMetricWidgetImageOutput> {

    private static final GetMetricWidgetImage $INSTANCE = new GetMetricWidgetImage();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetMetricWidgetImage"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetMetricWidgetImage instance() {
        return $INSTANCE;
    }

    private GetMetricWidgetImage() {}

    @Override
    public ShapeBuilder<GetMetricWidgetImageInput> inputBuilder() {
        return GetMetricWidgetImageInput.builder();
    }

    @Override
    public ShapeBuilder<GetMetricWidgetImageOutput> outputBuilder() {
        return GetMetricWidgetImageOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetMetricWidgetImageInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetMetricWidgetImageOutput.$SCHEMA;
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
