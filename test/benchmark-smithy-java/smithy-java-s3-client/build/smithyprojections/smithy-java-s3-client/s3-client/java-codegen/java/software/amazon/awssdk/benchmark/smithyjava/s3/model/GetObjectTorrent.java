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
 * <p>Returns torrent files from a bucket. BitTorrent can save you bandwidth when you're distributing large files.
 *
 * <p>You can get torrent only for objects that are less than 5 GB in size, and that are not encrypted using server-side
 * encryption with a customer-provided encryption key.
 *
 * <p>To use GET, you must have READ access to the object.
 *
 * <p>This functionality is not supported for Amazon S3 on Outposts.
 *
 * <p>The following action is related to <code>GetObjectTorrent</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To retrieve torrent files for an object</h3>
 *
 * <p>The following example retrieves torrent files of an object.{@snippet :
 * var input = GetObjectTorrentInput.builder()
 *                 .bucket("examplebucket").key("HappyFace.jpg")
 *                 .build();
 *
 * var result = client.getObjectTorrent(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class GetObjectTorrent implements ApiOperation<GetObjectTorrentInput, GetObjectTorrentOutput> {

    private static final GetObjectTorrent $INSTANCE = new GetObjectTorrent();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#GetObjectTorrent"),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?torrent")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema OUTPUT_STREAM_MEMBER = GetObjectTorrentOutput.$SCHEMA.member("Body");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetObjectTorrent instance() {
        return $INSTANCE;
    }

    private GetObjectTorrent() {}

    @Override
    public ShapeBuilder<GetObjectTorrentInput> inputBuilder() {
        return GetObjectTorrentInput.builder();
    }

    @Override
    public ShapeBuilder<GetObjectTorrentOutput> outputBuilder() {
        return GetObjectTorrentOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetObjectTorrentInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetObjectTorrentOutput.$SCHEMA;
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
        return OUTPUT_STREAM_MEMBER;
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
