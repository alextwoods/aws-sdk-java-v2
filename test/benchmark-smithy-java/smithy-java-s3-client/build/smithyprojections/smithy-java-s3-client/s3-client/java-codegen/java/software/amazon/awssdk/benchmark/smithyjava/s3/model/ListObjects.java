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
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Returns some or all (up to 1,000) of the objects in a bucket. You can use the request parameters as selection
 * criteria to return a subset of the objects in a bucket. A 200 OK response can contain valid or invalid XML. Be sure
 * to design your application to parse the contents of the response and handle it appropriately.
 *
 * <p>This action has been revised. We recommend that you use the newer version, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html">ListObjectsV2</a>, when developing
 * applications. For backward compatibility, Amazon S3 continues to support <code>ListObjects</code>.
 *
 * <p>The following operations are related to <code>ListObjects</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html">ListObjectsV2</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html">ListBuckets</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To list objects in a bucket</h3>
 *
 * <p>The following example list two objects in a bucket.{@snippet :
 * var input = ListObjectsInput.builder()
 *                 .bucket("examplebucket").maxKeys(2)
 *                 .build();
 *
 * var result = client.listObjects(input);
 * result.equals(ListObjectsOutput.builder()
 *                   .nextMarker("eyJNYXJrZXIiOiBudWxsLCAiYm90b190cnVuY2F0ZV9hbW91bnQiOiAyfQ==").contents(List.of(
 *                                 ObjectShape.builder()
 *                                     .lastModified(Instant.parse("2014-11-21T19:40:05Z")).eTag("\"70ee1738b6b21e2c8a43f3a5ab0eee71\"").storageClass(ObjectStorageClass.STANDARD).key("example1.jpg").owner(Owner.builder()
 *                                                .displayName("myname").id("12345example25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                .build()).size(11)
 *                                     .build()
 *                                 ,
 *                                 ObjectShape.builder()
 *                                     .lastModified(Instant.parse("2013-11-15T01:10:49Z")).eTag("\"9c8af9a76df052144598c115ef33e511\"").storageClass(ObjectStorageClass.STANDARD).key("example2.jpg").owner(Owner.builder()
 *                                                .displayName("myname").id("12345example25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                .build()).size(713193)
 *                                     .build()
 *                             ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class ListObjects implements ApiOperation<ListObjectsInput, ListObjectsOutput> {

    private static final ListObjects $INSTANCE = new ListObjects();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#ListObjects"),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(NoSuchBucket.$ID, NoSuchBucket.class, NoSuchBucket::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListObjects instance() {
        return $INSTANCE;
    }

    private ListObjects() {}

    @Override
    public ShapeBuilder<ListObjectsInput> inputBuilder() {
        return ListObjectsInput.builder();
    }

    @Override
    public ShapeBuilder<ListObjectsOutput> outputBuilder() {
        return ListObjectsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListObjectsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListObjectsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(NoSuchBucket.$SCHEMA);
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
