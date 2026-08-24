package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Service API schema
 */
@SmithyGenerated
public final class S3ApiService implements ApiService {
    private static final S3ApiService $INSTANCE = new S3ApiService();
    private static final Schema $SCHEMA = Schema.createService(ShapeId.from("com.amazonaws.s3#AmazonS3"),
        XmlNamespaceTrait.builder().uri("http://s3.amazonaws.com/doc/2006-03-01/").build());

    /**
     * Get an instance of this {@code ApiService}.
     *
     * @return An instance of this class.
     */
    public static S3ApiService instance() {
        return $INSTANCE;
    }

    private S3ApiService() {}

    @Override
    public Schema schema() {
        return $SCHEMA;
    }
}
