package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Service API schema
 */
@SmithyGenerated
public final class DynamoDBApiService implements ApiService {
    private static final DynamoDBApiService $INSTANCE = new DynamoDBApiService();
    private static final Schema $SCHEMA = Schema.createService(ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810"),
        XmlNamespaceTrait.builder().uri("http://dynamodb.amazonaws.com/doc/2012-08-10/").build());

    /**
     * Get an instance of this {@code ApiService}.
     *
     * @return An instance of this class.
     */
    public static DynamoDBApiService instance() {
        return $INSTANCE;
    }

    private DynamoDBApiService() {}

    @Override
    public Schema schema() {
        return $SCHEMA;
    }
}
