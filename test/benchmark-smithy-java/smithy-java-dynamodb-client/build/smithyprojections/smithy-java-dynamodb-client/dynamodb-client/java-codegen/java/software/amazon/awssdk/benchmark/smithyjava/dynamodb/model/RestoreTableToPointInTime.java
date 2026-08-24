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
 * Restores the specified table to the specified point in time within <code>EarliestRestorableDateTime</code> and <code>
 * LatestRestorableDateTime</code>. You can restore your table to any point in time in the last 35 days. You can set the
 * recovery period to any value between 1 and 35 days. Any number of users can execute up to 50 concurrent restores (any
 * type of restore) in a given account.
 *
 * <p>When you restore using point in time recovery, DynamoDB restores your table data to the state based on the
 * selected date and time (day:hour:minute:second) to a new table.
 *
 * <p>Along with data, the following are also included on the new restored table using point in time recovery:
 *
 * <ul>
 *   <li>
 *     Global secondary indexes (GSIs)
 *   </li>
 *   <li>
 *     Local secondary indexes (LSIs)
 *   </li>
 *   <li>
 *     Provisioned read and write capacity
 *   </li>
 *   <li>
 *     Encryption settings All these settings come from the current settings of the source table at the time of
 *     restore.
 *   </li>
 * </ul>
 *
 * <p>You must manually set up the following on the restored table:
 *
 * <ul>
 *   <li>
 *     Auto scaling policies
 *   </li>
 *   <li>
 *     IAM policies
 *   </li>
 *   <li>
 *     Amazon CloudWatch metrics and alarms
 *   </li>
 *   <li>
 *     Tags
 *   </li>
 *   <li>
 *     Stream settings
 *   </li>
 *   <li>
 *     Time to Live (TTL) settings
 *   </li>
 *   <li>
 *     Point in time recovery settings
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class RestoreTableToPointInTime implements ApiOperation<RestoreTableToPointInTimeInput, RestoreTableToPointInTimeOutput> {

    private static final RestoreTableToPointInTime $INSTANCE = new RestoreTableToPointInTime();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#RestoreTableToPointInTime"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(InvalidRestoreTimeException.$ID, InvalidRestoreTimeException.class, InvalidRestoreTimeException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(PointInTimeRecoveryUnavailableException.$ID, PointInTimeRecoveryUnavailableException.class, PointInTimeRecoveryUnavailableException::builder)
        .putType(TableAlreadyExistsException.$ID, TableAlreadyExistsException.class, TableAlreadyExistsException::builder)
        .putType(TableInUseException.$ID, TableInUseException.class, TableInUseException::builder)
        .putType(TableNotFoundException.$ID, TableNotFoundException.class, TableNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static RestoreTableToPointInTime instance() {
        return $INSTANCE;
    }

    private RestoreTableToPointInTime() {}

    @Override
    public ShapeBuilder<RestoreTableToPointInTimeInput> inputBuilder() {
        return RestoreTableToPointInTimeInput.builder();
    }

    @Override
    public ShapeBuilder<RestoreTableToPointInTimeOutput> outputBuilder() {
        return RestoreTableToPointInTimeOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return RestoreTableToPointInTimeInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return RestoreTableToPointInTimeOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, InvalidRestoreTimeException.$SCHEMA, LimitExceededException.$SCHEMA, PointInTimeRecoveryUnavailableException.$SCHEMA, TableAlreadyExistsException.$SCHEMA, TableInUseException.$SCHEMA, TableNotFoundException.$SCHEMA);
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
