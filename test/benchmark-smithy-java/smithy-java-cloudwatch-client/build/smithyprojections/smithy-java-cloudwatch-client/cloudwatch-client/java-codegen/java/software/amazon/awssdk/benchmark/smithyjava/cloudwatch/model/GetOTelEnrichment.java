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
 * Returns the current status of vended metric enrichment for the account, including whether CloudWatch vended metrics
 * are enriched with resource ARN and resource tag labels and queryable using PromQL. For the list of supported
 * resources, see <a
 * href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/UsingResourceTagsForTelemetry.html">Supported
 * Amazon Web Services infrastructure metrics</a>.
 */
@SmithyGenerated
public final class GetOTelEnrichment implements ApiOperation<GetOTelEnrichmentInput, GetOTelEnrichmentOutput> {

    private static final GetOTelEnrichment $INSTANCE = new GetOTelEnrichment();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetOTelEnrichment"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetOTelEnrichment instance() {
        return $INSTANCE;
    }

    private GetOTelEnrichment() {}

    @Override
    public ShapeBuilder<GetOTelEnrichmentInput> inputBuilder() {
        return GetOTelEnrichmentInput.builder();
    }

    @Override
    public ShapeBuilder<GetOTelEnrichmentOutput> outputBuilder() {
        return GetOTelEnrichmentOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetOTelEnrichmentInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetOTelEnrichmentOutput.$SCHEMA;
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
