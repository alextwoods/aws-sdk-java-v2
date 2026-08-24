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
 * Checks the status of continuous backups and point in time recovery on the specified table. Continuous backups are <code>
 * ENABLED</code> on all tables at table creation. If point in time recovery is enabled, <code>PointInTimeRecoveryStatus</code>
 * will be set to ENABLED.
 *
 * <p> After continuous backups and point in time recovery are enabled, you can restore to any point in time within <code>
 * EarliestRestorableDateTime</code> and <code>LatestRestorableDateTime</code>.
 *
 * <p><code>LatestRestorableDateTime</code> is typically 5 minutes before the current time. You can restore your table
 * to any point in time in the last 35 days. You can set the recovery period to any value between 1 and 35 days.
 *
 * <p>You can call <code>DescribeContinuousBackups</code> at a maximum rate of 10 times per second.
 */
@SmithyGenerated
public final class DescribeContinuousBackups implements ApiOperation<DescribeContinuousBackupsInput, DescribeContinuousBackupsOutput> {

    private static final DescribeContinuousBackups $INSTANCE = new DescribeContinuousBackups();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DescribeContinuousBackups"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(TableNotFoundException.$ID, TableNotFoundException.class, TableNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeContinuousBackups instance() {
        return $INSTANCE;
    }

    private DescribeContinuousBackups() {}

    @Override
    public ShapeBuilder<DescribeContinuousBackupsInput> inputBuilder() {
        return DescribeContinuousBackupsInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeContinuousBackupsOutput> outputBuilder() {
        return DescribeContinuousBackupsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeContinuousBackupsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeContinuousBackupsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, TableNotFoundException.$SCHEMA);
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
