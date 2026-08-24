package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.traits.OperationContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * <code>TransactGetItems</code> is a synchronous operation that atomically retrieves multiple items from one or more
 * tables (but not from indexes) in a single account and Region. A <code>TransactGetItems</code> call can contain up to
 * 100 <code>TransactGetItem</code> objects, each of which contains a <code>Get</code> structure that specifies an item
 * to retrieve from a table in the account and Region. A call to <code>TransactGetItems</code> cannot retrieve items
 * from tables in more than one Amazon Web Services account or Region. The aggregate size of the items in the
 * transaction cannot exceed 4 MB.
 *
 * <p>DynamoDB rejects the entire <code>TransactGetItems</code> request if any of the following is true:
 *
 * <ul>
 *   <li>
 *     A conflicting operation is in the process of updating an item to be read.
 *   </li>
 *   <li>
 *     There is insufficient provisioned capacity for the transaction to be completed.
 *   </li>
 *   <li>
 *     There is a user error, such as an invalid data format.
 *   </li>
 *   <li>
 *     The aggregate size of the items in the transaction exceeded 4 MB.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class TransactGetItems implements ApiOperation<TransactGetItemsInput, TransactGetItemsOutput> {

    private static final TransactGetItems $INSTANCE = new TransactGetItems();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#TransactGetItems"),
            new OperationContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#operationContextParams"),
                Node.objectNodeBuilder()
                    .withMember("ResourceArnList", Node.objectNodeBuilder()
                        .withMember("path", "TransactItems[*].Get.TableName")
                        .build())
                    .build()
            ));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(ProvisionedThroughputExceededException.$ID, ProvisionedThroughputExceededException.class, ProvisionedThroughputExceededException::builder)
        .putType(RequestLimitExceeded.$ID, RequestLimitExceeded.class, RequestLimitExceeded::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .putType(ThrottlingException.$ID, ThrottlingException.class, ThrottlingException::builder)
        .putType(TransactionCanceledException.$ID, TransactionCanceledException.class, TransactionCanceledException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static TransactGetItems instance() {
        return $INSTANCE;
    }

    private TransactGetItems() {}

    @Override
    public ShapeBuilder<TransactGetItemsInput> inputBuilder() {
        return TransactGetItemsInput.builder();
    }

    @Override
    public ShapeBuilder<TransactGetItemsOutput> outputBuilder() {
        return TransactGetItemsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return TransactGetItemsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return TransactGetItemsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, ProvisionedThroughputExceededException.$SCHEMA, RequestLimitExceeded.$SCHEMA, ResourceNotFoundException.$SCHEMA, ThrottlingException.$SCHEMA, TransactionCanceledException.$SCHEMA);
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
