package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Service API schema
 */
@SmithyGenerated
public final class CloudWatchApiService implements ApiService {
    private static final CloudWatchApiService $INSTANCE = new CloudWatchApiService();
    private static final Schema $SCHEMA = Schema.createService(ShapeId.from("com.amazonaws.cloudwatch#GraniteServiceVersion20100801"),
        XmlNamespaceTrait.builder().uri("http://monitoring.amazonaws.com/doc/2010-08-01/").build());

    /**
     * Get an instance of this {@code ApiService}.
     *
     * @return An instance of this class.
     */
    public static CloudWatchApiService instance() {
        return $INSTANCE;
    }

    private CloudWatchApiService() {}

    @Override
    public Schema schema() {
        return $SCHEMA;
    }
}
