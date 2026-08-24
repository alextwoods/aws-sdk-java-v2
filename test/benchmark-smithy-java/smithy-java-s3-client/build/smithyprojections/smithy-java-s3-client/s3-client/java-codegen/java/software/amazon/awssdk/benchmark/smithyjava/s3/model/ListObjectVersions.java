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
 * <p>Returns metadata about all versions of the objects in a bucket. You can also use request parameters as selection
 * criteria to return metadata about a subset of all the object versions.
 *
 * <p> To use this operation, you must have permission to perform the <code>s3:ListBucketVersions</code> action. Be
 * aware of the name difference.
 *
 * <p> A <code>200 OK</code> response can contain valid or invalid XML. Make sure to design your application to parse
 * the contents of the response and handle it appropriately.
 *
 * <p>To use this operation, you must have READ access to the bucket.
 *
 * <p>The following operations are related to <code>ListObjectVersions</code>:
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
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html">DeleteObject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To list object versions</h3>
 *
 * <p>The following example returns versions of an object with specific key name prefix.{@snippet :
 * var input = ListObjectVersionsInput.builder()
 *                 .bucket("examplebucket").prefix("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.listObjectVersions(input);
 * result.equals(ListObjectVersionsOutput.builder()
 *                   .versions(List.of(
 *                                 ObjectVersion.builder()
 *                                     .lastModified(Instant.parse("2016-12-15T01:19:41Z")).versionId("null").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"").storageClass(ObjectVersionStorageClass.STANDARD).key("HappyFace.jpg").owner(Owner.builder()
 *                                                .displayName("owner-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                .build()).isLatest(true).size(3191)
 *                                     .build()
 *                                 ,
 *                                 ObjectVersion.builder()
 *                                     .lastModified(Instant.parse("2016-12-13T00:58:26Z")).versionId("PHtexPGjH2y.zBgT8LmB7wwLI2mpbz.k").eTag("\"6805f2cfc46c0f04559748bb039d69ae\"").storageClass(ObjectVersionStorageClass.STANDARD).key("HappyFace.jpg").owner(Owner.builder()
 *                                                .displayName("owner-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                .build()).isLatest(false).size(3191)
 *                                     .build()
 *                             ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class ListObjectVersions implements ApiOperation<ListObjectVersionsInput, ListObjectVersionsOutput> {

    private static final ListObjectVersions $INSTANCE = new ListObjectVersions();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#ListObjectVersions"),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?versions")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListObjectVersions instance() {
        return $INSTANCE;
    }

    private ListObjectVersions() {}

    @Override
    public ShapeBuilder<ListObjectVersionsInput> inputBuilder() {
        return ListObjectVersionsInput.builder();
    }

    @Override
    public ShapeBuilder<ListObjectVersionsOutput> outputBuilder() {
        return ListObjectVersionsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListObjectVersionsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListObjectVersionsOutput.$SCHEMA;
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
