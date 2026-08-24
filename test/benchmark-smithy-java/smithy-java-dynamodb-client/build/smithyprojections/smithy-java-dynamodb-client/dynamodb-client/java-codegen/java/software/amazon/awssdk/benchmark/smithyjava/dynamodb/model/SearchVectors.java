package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Performs a vector similarity search on a vector index associated with an Amazon DynamoDB table, and returns the most
 * similar items sorted by similarity score based on the distance function configured for the index.
 *
 * <p>Score interpretation depends on the distance function:
 *
 * <ul>
 *   <li>
 *     <code>COSINE</code> - Returns the items with the <i>k smallest</i> scores. Scores range from 0 (identical) to
 *     2 (opposite). Lower scores indicate higher similarity.
 *   </li>
 *   <li>
 *     <code>EUCLIDEAN</code> - Returns the items with the <i>k smallest</i> scores. Scores represent the Euclidean
 *     distance between vectors. Lower scores indicate higher similarity.
 *   </li>
 *   <li>
 *     <code>DOT_PRODUCT</code> - Returns the items with the <i>k highest</i> scores. Higher scores indicate higher
 *     similarity.
 *   </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <h3>To search for similar vectors</h3>
 *
 * <p>This example searches the Products table for the top 3 items most similar to a provided vector, using the 'cosine-product-idx' vector index. The SearchConditionExpression filters results to the 'Electronics' category. The operation returns only the ProductName and Price attributes.{@snippet :
 * var input = SearchVectorsInput.builder()
 *                 .tableName("Products").indexName("cosine-product-idx").searchVector(List.of(
 *                                   AttributeValue.NMember("0.12"),
 *                                   AttributeValue.NMember("0.85"),
 *                                   AttributeValue.NMember("0.44"),
 *                                   AttributeValue.NMember("0.67")
 *                               )).topK(3).searchConditionExpression("Category = :cat").projectionExpression("ProductName, Price").expressionAttributeValues(Map.of(":cat", AttributeValue.SMember("Electronics"))).returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
 *                 .build();
 *
 * var result = client.searchVectors(input);
 * result.equals(SearchVectorsOutput.builder()
 *                   .searchResults(List.of(
 *                                      SearchResultItem.builder()
 *                                          .item(Map.of(
 *                                                    "ProductName", AttributeValue.SMember("Wireless Headphones"),
 *                                                    "Price", AttributeValue.NMember("79.99")
 *                                                )).score(0.95)
 *                                          .build()
 *                                      ,
 *                                      SearchResultItem.builder()
 *                                          .item(Map.of(
 *                                                    "ProductName", AttributeValue.SMember("Bluetooth Speaker"),
 *                                                    "Price", AttributeValue.NMember("49.99")
 *                                                )).score(0.87)
 *                                          .build()
 *                                      ,
 *                                      SearchResultItem.builder()
 *                                          .item(Map.of(
 *                                                    "ProductName", AttributeValue.SMember("USB-C Hub"),
 *                                                    "Price", AttributeValue.NMember("34.99")
 *                                                )).score(0.82)
 *                                          .build()
 *                                  )).consumedCapacity(VectorCapacity.builder()
 *                                         .vectorSearchRequestBytes(1024)
 *                                         .build())
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class SearchVectors implements ApiOperation<SearchVectorsInput, SearchVectorsOutput> {

    private static final SearchVectors $INSTANCE = new SearchVectors();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#SearchVectors"),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("IsSearchOperation", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(RequestLimitExceeded.$ID, RequestLimitExceeded.class, RequestLimitExceeded::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .putType(ThrottlingException.$ID, ThrottlingException.class, ThrottlingException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static SearchVectors instance() {
        return $INSTANCE;
    }

    private SearchVectors() {}

    @Override
    public ShapeBuilder<SearchVectorsInput> inputBuilder() {
        return SearchVectorsInput.builder();
    }

    @Override
    public ShapeBuilder<SearchVectorsOutput> outputBuilder() {
        return SearchVectorsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return SearchVectorsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return SearchVectorsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, RequestLimitExceeded.$SCHEMA, ResourceNotFoundException.$SCHEMA, ThrottlingException.$SCHEMA);
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
