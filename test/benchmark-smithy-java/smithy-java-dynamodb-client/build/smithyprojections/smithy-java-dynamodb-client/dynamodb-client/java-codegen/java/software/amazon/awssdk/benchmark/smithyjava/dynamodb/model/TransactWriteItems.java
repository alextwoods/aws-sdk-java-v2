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
 * <code>TransactWriteItems</code> is a synchronous write operation that groups up to 100 action requests. These actions
 * can target items in different tables, but not in different Amazon Web Services accounts or Regions, and no two
 * actions can target the same item. For example, you cannot both <code>ConditionCheck</code> and <code>Update</code>
 * the same item. The aggregate size of the items in the transaction cannot exceed 4 MB.
 *
 * <p>The actions are completed atomically so that either all of them succeed, or all of them fail. They are defined by
 * the following objects:
 *
 * <ul>
 *   <li>
 *     <code>Put</code>  —   Initiates a <code>PutItem</code> operation to write a new item. This structure
 *     specifies the primary key of the item to be written, the name of the table to write it in, an optional
 *     condition expression that must be satisfied for the write to succeed, a list of the item's attributes, and a
 *     field indicating whether to retrieve the item's attributes if the condition is not met.
 *   </li>
 *   <li>
 *     <code>Update</code>  —   Initiates an <code>UpdateItem</code> operation to update an existing item. This
 *     structure specifies the primary key of the item to be updated, the name of the table where it resides, an
 *     optional condition expression that must be satisfied for the update to succeed, an expression that defines
 *     one or more attributes to be updated, and a field indicating whether to retrieve the item's attributes if the
 *     condition is not met.
 *   </li>
 *   <li>
 *     <code>Delete</code>  —   Initiates a <code>DeleteItem</code> operation to delete an existing item. This
 *     structure specifies the primary key of the item to be deleted, the name of the table where it resides, an
 *     optional condition expression that must be satisfied for the deletion to succeed, and a field indicating
 *     whether to retrieve the item's attributes if the condition is not met.
 *   </li>
 *   <li>
 *     <code>ConditionCheck</code>  —   Applies a condition to an item that is not being modified by the
 *     transaction. This structure specifies the primary key of the item to be checked, the name of the table where
 *     it resides, a condition expression that must be satisfied for the transaction to succeed, and a field
 *     indicating whether to retrieve the item's attributes if the condition is not met.
 *   </li>
 * </ul>
 *
 * <p>DynamoDB rejects the entire <code>TransactWriteItems</code> request if any of the following is true:
 *
 * <ul>
 *   <li>
 *     A condition in one of the condition expressions is not met.
 *   </li>
 *   <li>
 *     An ongoing operation is in the process of updating the same item.
 *   </li>
 *   <li>
 *     There is insufficient provisioned capacity for the transaction to be completed.
 *   </li>
 *   <li>
 *     An item size becomes too large (bigger than 400 KB), a local secondary index (LSI) becomes too large, or a
 *     similar validation error occurs because of changes made by the transaction.
 *   </li>
 *   <li>
 *     The aggregate size of the items in the transaction exceeds 4 MB.
 *   </li>
 *   <li>
 *     There is a user error, such as an invalid data format.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class TransactWriteItems implements ApiOperation<TransactWriteItemsInput, TransactWriteItemsOutput> {

    private static final TransactWriteItems $INSTANCE = new TransactWriteItems();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#TransactWriteItems"),
            new OperationContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#operationContextParams"),
                Node.objectNodeBuilder()
                    .withMember("ResourceArnList", Node.objectNodeBuilder()
                        .withMember("path", "TransactItems[*].[ConditionCheck.TableName, Put.TableName, Delete.TableName, Update.TableName][]")
                        .build())
                    .build()
            ));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(IdempotentParameterMismatchException.$ID, IdempotentParameterMismatchException.class, IdempotentParameterMismatchException::builder)
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .putType(ProvisionedThroughputExceededException.$ID, ProvisionedThroughputExceededException.class, ProvisionedThroughputExceededException::builder)
        .putType(RequestLimitExceeded.$ID, RequestLimitExceeded.class, RequestLimitExceeded::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .putType(ThrottlingException.$ID, ThrottlingException.class, ThrottlingException::builder)
        .putType(TransactionCanceledException.$ID, TransactionCanceledException.class, TransactionCanceledException::builder)
        .putType(TransactionInProgressException.$ID, TransactionInProgressException.class, TransactionInProgressException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema IDEMPOTENCY_TOKEN_MEMBER = TransactWriteItemsInput.$SCHEMA.member("ClientRequestToken");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static TransactWriteItems instance() {
        return $INSTANCE;
    }

    private TransactWriteItems() {}

    @Override
    public ShapeBuilder<TransactWriteItemsInput> inputBuilder() {
        return TransactWriteItemsInput.builder();
    }

    @Override
    public ShapeBuilder<TransactWriteItemsOutput> outputBuilder() {
        return TransactWriteItemsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return TransactWriteItemsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return TransactWriteItemsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(IdempotentParameterMismatchException.$SCHEMA, InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA, ProvisionedThroughputExceededException.$SCHEMA, RequestLimitExceeded.$SCHEMA, ResourceNotFoundException.$SCHEMA, ThrottlingException.$SCHEMA, TransactionCanceledException.$SCHEMA, TransactionInProgressException.$SCHEMA);
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
        return IDEMPOTENCY_TOKEN_MEMBER;
    }

    @Override
    public ApiService service() {
        return DynamoDBApiService.instance();
    }
    }
