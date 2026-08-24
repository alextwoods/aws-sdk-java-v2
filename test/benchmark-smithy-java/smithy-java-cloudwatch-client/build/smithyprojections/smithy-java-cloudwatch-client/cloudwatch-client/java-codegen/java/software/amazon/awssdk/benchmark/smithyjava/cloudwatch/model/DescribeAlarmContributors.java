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
 * Returns the information of the current alarm contributors that are in <code>ALARM</code> state. This operation
 * returns details about the individual time series that contribute to the alarm's state.
 */
@SmithyGenerated
public final class DescribeAlarmContributors implements ApiOperation<DescribeAlarmContributorsInput, DescribeAlarmContributorsOutput> {

    private static final DescribeAlarmContributors $INSTANCE = new DescribeAlarmContributors();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DescribeAlarmContributors"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InvalidNextToken.$ID, InvalidNextToken.class, InvalidNextToken::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeAlarmContributors instance() {
        return $INSTANCE;
    }

    private DescribeAlarmContributors() {}

    @Override
    public ShapeBuilder<DescribeAlarmContributorsInput> inputBuilder() {
        return DescribeAlarmContributorsInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeAlarmContributorsOutput> outputBuilder() {
        return DescribeAlarmContributorsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeAlarmContributorsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeAlarmContributorsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InvalidNextToken.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
