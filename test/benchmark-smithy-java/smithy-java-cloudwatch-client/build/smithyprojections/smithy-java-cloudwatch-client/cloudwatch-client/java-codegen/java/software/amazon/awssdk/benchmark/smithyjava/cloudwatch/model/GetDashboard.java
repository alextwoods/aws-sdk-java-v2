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
 * Displays the details of the dashboard that you specify.
 *
 * <p>To copy an existing dashboard, use <code>GetDashboard</code>, and then use the data returned within <code>
 * DashboardBody</code> as the template for the new dashboard when you call <code>PutDashboard</code> to create the
 * copy.
 */
@SmithyGenerated
public final class GetDashboard implements ApiOperation<GetDashboardInput, GetDashboardOutput> {

    private static final GetDashboard $INSTANCE = new GetDashboard();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetDashboard"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(DashboardNotFoundError.$ID, DashboardNotFoundError.class, DashboardNotFoundError::builder)
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetDashboard instance() {
        return $INSTANCE;
    }

    private GetDashboard() {}

    @Override
    public ShapeBuilder<GetDashboardInput> inputBuilder() {
        return GetDashboardInput.builder();
    }

    @Override
    public ShapeBuilder<GetDashboardOutput> outputBuilder() {
        return GetDashboardOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetDashboardInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetDashboardOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(DashboardNotFoundError.$SCHEMA, InternalServiceFault.$SCHEMA, InvalidParameterValueException.$SCHEMA);
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
