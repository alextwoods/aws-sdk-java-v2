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
 * The <code>DeleteTable</code> operation deletes a table and all of its items. After a <code>DeleteTable</code>
 * request, the specified table is in the <code>DELETING</code> state until DynamoDB completes the deletion. If the
 * table is in the <code>ACTIVE</code> state, you can delete it. If a table is in <code>CREATING</code> or <code>
 * UPDATING</code> states, then DynamoDB returns a <code>ResourceInUseException</code>. If the specified table does not
 * exist, DynamoDB returns a <code>ResourceNotFoundException</code>. If table is already in the <code>DELETING</code>
 * state, no error is returned.
 *
 * <p>DynamoDB might continue to accept data read and write operations, such as <code>GetItem</code> and <code>PutItem</code>
 * , on a table in the <code>DELETING</code> state until the table deletion is complete. For the full list of table
 * states, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TableDescription.html#DDB-Type-TableDescription-TableStatus">TableStatus</a>.
 *
 * <p>When you delete a table, any indexes on that table are also deleted.
 *
 * <p>If you have DynamoDB Streams enabled on the table, then the corresponding stream on that table goes into the <code>
 * DISABLED</code> state, and the stream is automatically deleted after 24 hours.
 *
 * <p>Use the <code>DescribeTable</code> action to check the status of the table.
 *
 * <h2>Examples</h2>
 * <h3>To delete a table</h3>
 *
 * <p>This example deletes the Music table.{@snippet :
 * var input = DeleteTableInput.builder()
 *                 .tableName("Music")
 *                 .build();
 *
 * var result = client.deleteTable(input);
 * result.equals(DeleteTableOutput.builder()
 *                   .tableDescription(TableDescription.builder()
 *                                         .tableStatus(TableStatus.DELETING).tableSizeBytes(0).itemCount(0).tableName("Music").provisionedThroughput(ProvisionedThroughputDescription.builder()
 *                                                                    .numberOfDecreasesToday(1).writeCapacityUnits(5).readCapacityUnits(5)
 *                                                                    .build())
 *                                         .build())
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class DeleteTable implements ApiOperation<DeleteTableInput, DeleteTableOutput> {

    private static final DeleteTable $INSTANCE = new DeleteTable();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DeleteTable"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(ResourceInUseException.$ID, ResourceInUseException.class, ResourceInUseException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DeleteTable instance() {
        return $INSTANCE;
    }

    private DeleteTable() {}

    @Override
    public ShapeBuilder<DeleteTableInput> inputBuilder() {
        return DeleteTableInput.builder();
    }

    @Override
    public ShapeBuilder<DeleteTableOutput> outputBuilder() {
        return DeleteTableOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DeleteTableInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DeleteTableOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, LimitExceededException.$SCHEMA, ResourceInUseException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
