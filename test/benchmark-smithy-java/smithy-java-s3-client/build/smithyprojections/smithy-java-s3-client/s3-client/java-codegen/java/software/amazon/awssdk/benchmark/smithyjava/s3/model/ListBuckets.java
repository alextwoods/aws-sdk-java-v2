package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Returns a list of all buckets owned by the authenticated sender of the request. To grant IAM permission to use
 * this operation, you must add the <code>s3:ListAllMyBuckets</code> policy action.
 *
 * <p>For information about Amazon S3 buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/creating-buckets-s3.html">Creating, configuring, and working with Amazon S3 buckets</a>.
 *
 * <p>We strongly recommend using only paginated <code>ListBuckets</code> requests. Unpaginated <code>ListBuckets</code>
 * requests are only supported for Amazon Web Services accounts set to the default general purpose bucket quota of
 * 10,000. If you have an approved general purpose bucket quota above 10,000, you must send paginated <code>ListBuckets</code>
 * requests to list your account’s buckets. All unpaginated <code>ListBuckets</code> requests will be rejected for
 * Amazon Web Services accounts with a general purpose bucket quota greater than 10,000.
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To list all buckets</h3>
 *
 * <p>The following example returns all the buckets owned by the sender of this request.{@snippet :
 * var input = ListBucketsInput.builder()
 *
 *                 .build();
 *
 * var result = client.listBuckets(input);
 * result.equals(ListBucketsOutput.builder()
 *                   .owner(Owner.builder()
 *                              .displayName("own-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31")
 *                              .build()).buckets(List.of(
 *                                Bucket.builder()
 *                                    .creationDate(Instant.parse("2012-02-15T21:03:02Z")).name("examplebucket")
 *                                    .build()
 *                                ,
 *                                Bucket.builder()
 *                                    .creationDate(Instant.parse("2011-07-24T19:33:50Z")).name("examplebucket2")
 *                                    .build()
 *                                ,
 *                                Bucket.builder()
 *                                    .creationDate(Instant.parse("2010-12-17T00:56:49Z")).name("examplebucket3")
 *                                    .build()
 *                            ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class ListBuckets implements ApiOperation<ListBucketsInput, ListBucketsOutput> {

    private static final ListBuckets $INSTANCE = new ListBuckets();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#ListBuckets"),
            PaginatedTrait.builder().inputToken("ContinuationToken").outputToken("ContinuationToken").items("Buckets").pageSize("MaxBuckets").build(),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/?x-id=ListBuckets")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListBuckets instance() {
        return $INSTANCE;
    }

    private ListBuckets() {}

    @Override
    public ShapeBuilder<ListBucketsInput> inputBuilder() {
        return ListBucketsInput.builder();
    }

    @Override
    public ShapeBuilder<ListBucketsOutput> outputBuilder() {
        return ListBucketsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListBucketsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListBucketsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of();
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
        return S3ApiService.instance();
    }
    }
