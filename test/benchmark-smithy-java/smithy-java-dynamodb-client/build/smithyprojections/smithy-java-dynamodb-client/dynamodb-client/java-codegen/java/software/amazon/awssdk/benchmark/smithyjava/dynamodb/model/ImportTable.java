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
 * Imports table data from an S3 bucket.
 */
@SmithyGenerated
public final class ImportTable implements ApiOperation<ImportTableInput, ImportTableOutput> {

    private static final ImportTable $INSTANCE = new ImportTable();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#ImportTable"),
            new OperationContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#operationContextParams"),
                Node.objectNodeBuilder()
                    .withMember("ResourceArn", Node.objectNodeBuilder()
                        .withMember("path", "TableCreationParameters.TableName")
                        .build())
                    .build()
            ));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ImportConflictException.$ID, ImportConflictException.class, ImportConflictException::builder)
        .putType(LimitExceededException.$ID, LimitExceededException.class, LimitExceededException::builder)
        .putType(ResourceInUseException.$ID, ResourceInUseException.class, ResourceInUseException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema IDEMPOTENCY_TOKEN_MEMBER = ImportTableInput.$SCHEMA.member("ClientToken");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ImportTable instance() {
        return $INSTANCE;
    }

    private ImportTable() {}

    @Override
    public ShapeBuilder<ImportTableInput> inputBuilder() {
        return ImportTableInput.builder();
    }

    @Override
    public ShapeBuilder<ImportTableOutput> outputBuilder() {
        return ImportTableOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ImportTableInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ImportTableOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ImportConflictException.$SCHEMA, LimitExceededException.$SCHEMA, ResourceInUseException.$SCHEMA);
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
