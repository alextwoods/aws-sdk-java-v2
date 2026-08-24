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
 * Deletes an existing backup of a table.
 *
 * <p>You can call <code>DeleteBackup</code> at a maximum rate of 10 times per second.
 */
@SmithyGenerated
public final class DeleteBackup implements ApiOperation<DeleteBackupInput, DeleteBackupOutput> {

    private static final DeleteBackup $INSTANCE = new DeleteBackup();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DeleteBackup"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(BackupInUseException.$ID, BackupInUseException.class, BackupInUseException::builder)
        .putType(BackupNotFoundException.$ID, BackupNotFoundException.class, BackupNotFoundException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteBackup instance() {
        return $INSTANCE;
    }

    private DeleteBackup() {}

    @Override
    public ShapeBuilder<DeleteBackupInput> inputBuilder() {
        return DeleteBackupInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteBackupOutput> outputBuilder() {
        return DeleteBackupOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteBackupInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteBackupOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(BackupInUseException.$SCHEMA, BackupNotFoundException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA);
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
