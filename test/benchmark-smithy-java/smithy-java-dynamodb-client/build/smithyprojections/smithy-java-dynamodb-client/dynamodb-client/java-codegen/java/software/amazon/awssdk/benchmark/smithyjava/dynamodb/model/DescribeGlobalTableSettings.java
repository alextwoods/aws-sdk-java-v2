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
 * Describes Region-specific settings for a global table.
 *
 * <p>This documentation is for version 2017.11.29 (Legacy) of global tables, which should be avoided for new global
 * tables. Customers should use <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html">Global Tables version 2019.11.21 (Current)</a> when possible, because it provides
 * greater flexibility, higher efficiency, and consumes less write capacity than 2017.11.29 (Legacy).
 *
 * <p>To determine which version you're using, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/globaltables.DetermineVersion.html">Determining the global table version you are using</a>. To update
 * existing global tables from version 2017.11.29 (Legacy) to version 2019.11.21 (Current), see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_upgrade.html">Upgrading global
 * tables</a>.
 */
@SmithyGenerated
public final class DescribeGlobalTableSettings implements ApiOperation<DescribeGlobalTableSettingsInput, DescribeGlobalTableSettingsOutput> {

    private static final DescribeGlobalTableSettings $INSTANCE = new DescribeGlobalTableSettings();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DescribeGlobalTableSettings"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(GlobalTableNotFoundException.$ID, GlobalTableNotFoundException.class, GlobalTableNotFoundException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeGlobalTableSettings instance() {
        return $INSTANCE;
    }

    private DescribeGlobalTableSettings() {}

    @Override
    public ShapeBuilder<DescribeGlobalTableSettingsInput> inputBuilder() {
        return DescribeGlobalTableSettingsInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeGlobalTableSettingsOutput> outputBuilder() {
        return DescribeGlobalTableSettingsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeGlobalTableSettingsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeGlobalTableSettingsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(GlobalTableNotFoundException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA);
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
