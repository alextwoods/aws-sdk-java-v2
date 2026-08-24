package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Updates the status for contributor insights for a specific table or index. CloudWatch Contributor Insights for
 * DynamoDB graphs display the partition key and (if applicable) sort key of frequently accessed items and frequently
 * throttled items in plaintext. If you require the use of Amazon Web Services Key Management Service (KMS) to encrypt
 * this table’s partition key and sort key data with an Amazon Web Services managed key or customer managed key, you
 * should not enable CloudWatch Contributor Insights for DynamoDB for this table.
 */
@SmithyGenerated
public final class UpdateContributorInsights implements ApiOperation<UpdateContributorInsightsInput, UpdateContributorInsightsOutput> {

    private static final UpdateContributorInsights $INSTANCE = new UpdateContributorInsights();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#UpdateContributorInsights"));

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
    public static UpdateContributorInsights instance() {
        return $INSTANCE;
    }

    private UpdateContributorInsights() {}

    @Override
    public ShapeBuilder<UpdateContributorInsightsInput> inputBuilder() {
        return UpdateContributorInsightsInput.builder();
    }

    @Override
    public ShapeBuilder<UpdateContributorInsightsOutput> outputBuilder() {
        return UpdateContributorInsightsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return UpdateContributorInsightsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return UpdateContributorInsightsOutput.$SCHEMA;
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
