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
 * Creates a new table from an existing backup. Any number of users can execute up to 50 concurrent restores (any type
 * of restore) in a given account.
 *
 * <p>You can call <code>RestoreTableFromBackup</code> at a maximum rate of 10 times per second.
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
 * </ul>
 */
@SmithyGenerated
public final class RestoreTableFromBackup implements ApiOperation<RestoreTableFromBackupInput, RestoreTableFromBackupOutput> {

    private static final RestoreTableFromBackup $INSTANCE = new RestoreTableFromBackup();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#RestoreTableFromBackup"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(BackupInUseException.$ID, BackupInUseException.class, BackupInUseException::builder)
        .putType(BackupNotFoundException.$ID, BackupNotFoundException.class, BackupNotFoundException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(TableAlreadyExistsException.$ID, TableAlreadyExistsException.class, TableAlreadyExistsException::builder)
        .putType(TableInUseException.$ID, TableInUseException.class, TableInUseException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static RestoreTableFromBackup instance() {
        return $INSTANCE;
    }

    private RestoreTableFromBackup() {}

    @Override
    public ShapeBuilder<RestoreTableFromBackupInput> inputBuilder() {
        return RestoreTableFromBackupInput.builder();
    }

    @Override
    public ShapeBuilder<RestoreTableFromBackupOutput> outputBuilder() {
        return RestoreTableFromBackupOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return RestoreTableFromBackupInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return RestoreTableFromBackupOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(BackupInUseException.$SCHEMA, BackupNotFoundException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, TableAlreadyExistsException.$SCHEMA, TableInUseException.$SCHEMA);
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
