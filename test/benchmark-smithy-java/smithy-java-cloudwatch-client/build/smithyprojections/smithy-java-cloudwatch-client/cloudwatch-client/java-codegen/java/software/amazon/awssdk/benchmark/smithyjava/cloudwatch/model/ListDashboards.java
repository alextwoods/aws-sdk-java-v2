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
 * Returns a list of the dashboards for your account. If you include <code>DashboardNamePrefix</code>, only those
 * dashboards with names starting with the prefix are listed. Otherwise, all dashboards in your account are listed.
 *
 * <p><code>ListDashboards</code> returns up to 1000 results on one page. If there are more than 1000 dashboards, you
 * can call <code>ListDashboards</code> again and include the value you received for <code>NextToken</code> in the first
 * call, to receive the next 1000 results.
 */
@SmithyGenerated
public final class ListDashboards implements ApiOperation<ListDashboardsInput, ListDashboardsOutput> {

    private static final ListDashboards $INSTANCE = new ListDashboards();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#ListDashboards"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").items("DashboardEntries").build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServiceFault.$ID, InternalServiceFault.class, InternalServiceFault::builder)
        .putType(InvalidParameterValueException.$ID, InvalidParameterValueException.class, InvalidParameterValueException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListDashboards instance() {
        return $INSTANCE;
    }

    private ListDashboards() {}

    @Override
    public ShapeBuilder<ListDashboardsInput> inputBuilder() {
        return ListDashboardsInput.builder();
    }

    @Override
    public ShapeBuilder<ListDashboardsOutput> outputBuilder() {
        return ListDashboardsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListDashboardsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListDashboardsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServiceFault.$SCHEMA, InvalidParameterValueException.$SCHEMA);
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
