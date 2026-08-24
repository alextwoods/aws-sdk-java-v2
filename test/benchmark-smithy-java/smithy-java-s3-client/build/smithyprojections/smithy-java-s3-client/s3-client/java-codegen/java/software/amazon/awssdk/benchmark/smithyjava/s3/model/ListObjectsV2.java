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
 * Returns some or all (up to 1,000) of the objects in a bucket with each request. You can use the request parameters as
 * selection criteria to return a subset of the objects in a bucket. A <code>200 OK</code> response can contain valid or
 * invalid XML. Make sure to design your application to parse the contents of the response and handle it appropriately.
 * For more information about listing objects, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ListingKeysUsingAPIs.html">Listing object keys programmatically</a> in the <i>Amazon S3 User
 * Guide</i>. To get a list of your buckets, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html">ListBuckets</a>.
 *
 * <ul>
 *   <li>
 *     <b>General purpose bucket</b> - For general purpose buckets, <code>ListObjectsV2</code> doesn't return
 *     prefixes that are related only to in-progress multipart uploads.
 *   </li>
 *   <li>
 *     <b>Directory buckets</b> - For directory buckets, <code>ListObjectsV2</code> response includes the prefixes
 *     that are related only to in-progress multipart uploads.
 *   </li>
 *   <li>
 *     <b>Directory buckets</b> - For directory buckets, you must make requests for this API operation to the Zonal
 *     endpoint. These endpoints support virtual-hosted-style requests in the format <code>https://<i>
 *     amzn-s3-demo-bucket</i>.s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com/<i>key-name</i></code>.
 *     Path-style requests are not supported. For more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">
 *     Regional and Zonal endpoints for directory buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>
 *     . For more information about endpoints in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a>
 *     in the <i>Amazon S3 User Guide</i>.
 *   </li>
 * </ul>
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - To use this operation, you must have READ access to the
 *         bucket. You must have permission to perform the <code>s3:ListBucket</code> action. The bucket owner
 *         has this permission by default and can grant this permission to others. For more information about
 *         permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-actions.html#using-with-s3-actions-related-to-bucket-subresources">Permissions Related to Bucket Subresource Operations</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-access-control.html">Managing Access
 *         Permissions to Your Amazon S3 Resources</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *       <li>
 *         <b>Directory bucket permissions</b> - To grant access to this API operation on a directory bucket, we
 *         recommend that you use the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateSession.html"><code>CreateSession</code></a> API operation for session-based
 *         authorization. Specifically, you grant the <code>s3express:CreateSession</code> permission to the
 *         directory bucket in a bucket policy or an IAM identity-based policy. Then, you make the <code>
 *         CreateSession</code> API call on the bucket to obtain a session token. With the session token in your
 *         request header, you can make API requests to this operation. After the session token expires, you
 *         make another <code>CreateSession</code> API call to generate a new session token for use. Amazon Web
 *         Services CLI or SDKs create session and refresh the session token automatically to avoid service
 *         interruptions when a session expires. For more information about authorization, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateSession.html"><code>
 *         CreateSession</code></a>.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Sorting order of returned objects
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket</b> - For general purpose buckets, <code>ListObjectsV2</code> returns
 *         objects in lexicographical order based on their key names.
 *       </li>
 *       <li>
 *         <b>Directory bucket</b> - For directory buckets, <code>ListObjectsV2</code> does not return objects
 *         in lexicographical order.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code><i>Bucket-name</i>.s3express-<i>zone-id</i>
 *     .<i>region-code</i>.amazonaws.com</code>.
 *   </dd>
 * </dl>
 *
 * <p>This section describes the latest revision of this action. We recommend that you use this revised API operation
 * for application development. For backward compatibility, Amazon S3 continues to support the prior version of this API
 * operation, <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html">ListObjects</a>.
 *
 * <p>The following operations are related to <code>ListObjectsV2</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html">PutObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html">CreateBucket</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To get object list</h3>
 *
 * <p>The following example retrieves object list. The request specifies max keys to limit response to include only 2 object keys.{@snippet :
 * var input = ListObjectsV2Input.builder()
 *                 .bucket("DOC-EXAMPLE-BUCKET").maxKeys(2)
 *                 .build();
 *
 * var result = client.listObjectsV2(input);
 * result.equals(ListObjectsV2Output.builder()
 *                   .name("DOC-EXAMPLE-BUCKET").maxKeys(2).prefix("").keyCount(2).nextContinuationToken("1w41l63U0xa8q7smH50vCxyTQqdxo69O3EmK28Bi5PcROI4wI/EyIJg==").isTruncated(true).contents(List.of(
 *                                 ObjectShape.builder()
 *                                     .lastModified(Instant.parse("2014-11-21T19:40:05Z")).eTag("\"70ee1738b6b21e2c8a43f3a5ab0eee71\"").storageClass(ObjectStorageClass.STANDARD).key("happyface.jpg").size(11)
 *                                     .build()
 *                                 ,
 *                                 ObjectShape.builder()
 *                                     .lastModified(Instant.parse("2014-05-02T04:51:50Z")).eTag("\"becf17f89c30367a9a44495d62ed521a-1\"").storageClass(ObjectStorageClass.STANDARD).key("test.jpg").size(4192256)
 *                                     .build()
 *                             ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class ListObjectsV2 implements ApiOperation<ListObjectsV2Input, ListObjectsV2Output> {

    private static final ListObjectsV2 $INSTANCE = new ListObjectsV2();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#ListObjectsV2"),
            PaginatedTrait.builder().inputToken("ContinuationToken").outputToken("NextContinuationToken").pageSize("MaxKeys").build(),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?list-type=2")).build());

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
    public static ListObjectsV2 instance() {
        return $INSTANCE;
    }

    private ListObjectsV2() {}

    @Override
    public ShapeBuilder<ListObjectsV2Input> inputBuilder() {
        return ListObjectsV2Input.builder();
    }

    @Override
    public ShapeBuilder<ListObjectsV2Output> outputBuilder() {
        return ListObjectsV2Output.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListObjectsV2Input.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListObjectsV2Output.$SCHEMA;
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
