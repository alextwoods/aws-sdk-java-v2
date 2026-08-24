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
 * Creates a dashboard if it does not already exist, or updates an existing dashboard. If you update a dashboard, the
 * entire contents are replaced with what you specify here.
 *
 * <p>All dashboards in your account are global, not region-specific.
 *
 * <p>A simple way to create a dashboard using <code>PutDashboard</code> is to copy an existing dashboard. To copy an
 * existing dashboard using the console, you can load the dashboard and then use the View/edit source command in the
 * Actions menu to display the JSON block for that dashboard. Another way to copy a dashboard is to use <code>
 * GetDashboard</code>, and then use the data returned within <code>DashboardBody</code> as the template for the new
 * dashboard when you call <code>PutDashboard</code>.
 *
 * <p>When you create a dashboard with <code>PutDashboard</code>, a good practice is to add a text widget at the top of
 * the dashboard with a message that the dashboard was created by script and should not be changed in the console. This
 * message could also point console users to the location of the <code>DashboardBody</code> script or the CloudFormation
 * template used to create the dashboard.
 */
@SmithyGenerated
public final class PutDashboard implements ApiOperation<PutDashboardInput, PutDashboardOutput> {

    private static final PutDashboard $INSTANCE = new PutDashboard();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#PutDashboard"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConflictException.$ID, ConflictException.class, ConflictException::builder)
        .putType(DashboardInvalidInputError.$ID, DashboardInvalidInputError.class, DashboardInvalidInputError::builder)
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutDashboard instance() {
        return $INSTANCE;
    }

    private PutDashboard() {}

    @Override
    public ShapeBuilder<PutDashboardInput> inputBuilder() {
        return PutDashboardInput.builder();
    }

    @Override
    public ShapeBuilder<PutDashboardOutput> outputBuilder() {
        return PutDashboardOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutDashboardInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutDashboardOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConflictException.$SCHEMA, DashboardInvalidInputError.$SCHEMA, InternalServiceFault.$SCHEMA);
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
