package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Returns a list of ContributorInsightsSummary for a table and all its global secondary indexes.
 */
@SmithyGenerated
public final class ListContributorInsights implements ApiOperation<ListContributorInsightsInput, ListContributorInsightsOutput> {

    private static final ListContributorInsights $INSTANCE = new ListContributorInsights();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#ListContributorInsights"),
            PaginatedTrait.builder().inputToken("NextToken").outputToken("NextToken").pageSize("MaxResults").build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListContributorInsights instance() {
        return $INSTANCE;
    }

    private ListContributorInsights() {}

    @Override
    public ShapeBuilder<ListContributorInsightsInput> inputBuilder() {
        return ListContributorInsightsInput.builder();
    }

    @Override
    public ShapeBuilder<ListContributorInsightsOutput> outputBuilder() {
        return ListContributorInsightsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListContributorInsightsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListContributorInsightsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
        return DynamoDBApiService.instance();
    }
    }
