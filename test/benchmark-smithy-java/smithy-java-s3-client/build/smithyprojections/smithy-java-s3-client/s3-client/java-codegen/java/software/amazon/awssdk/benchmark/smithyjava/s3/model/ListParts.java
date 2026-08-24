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
 * Lists the parts that have been uploaded for a specific multipart upload.
 *
 * <p>To use this operation, you must provide the <code>upload ID</code> in the request. You obtain this uploadID by
 * sending the initiate multipart upload request through <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>.
 *
 * <p>The <code>ListParts</code> request returns a maximum of 1,000 uploaded parts. The limit of 1,000 parts is also the
 * default value. You can restrict the number of parts in a response by specifying the <code>max-parts</code> request
 * parameter. If your multipart upload consists of more than 1,000 parts, the response returns an <code>IsTruncated</code>
 * field with the value of <code>true</code>, and a <code>NextPartNumberMarker</code> element. To list remaining
 * uploaded parts, in subsequent <code>ListParts</code> requests, include the <code>part-number-marker</code> query
 * string parameter and set its value to the <code>NextPartNumberMarker</code> field value from the previous response.
 *
 * <p>For more information on multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/uploadobjusingmpu.html">Uploading Objects Using Multipart Upload</a> in the <i>Amazon S3
 * User Guide</i>.
 *
 * <p><b>Directory buckets</b> - For directory buckets, you must make requests for this API operation to the Zonal
 * endpoint. These endpoints support virtual-hosted-style requests in the format <code>https://<i>amzn-s3-demo-bucket</i>
 * .s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com/<i>key-name</i></code>. Path-style requests are not
 * supported. For more information about endpoints in Availability Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/endpoint-directory-buckets-AZ.html">Regional and Zonal endpoints for
 * directory buckets in Availability Zones</a> in the <i>Amazon S3 User Guide</i>. For more information about endpoints
 * in Local Zones, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-lzs-for-directory-buckets.html">Concepts for directory buckets in Local Zones</a> in the <i>Amazon S3 User Guide</i>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket permissions</b> - For information about permissions required to use the
 *         multipart upload API, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/mpuAndPermissions.html">Multipart Upload and Permissions</a> in the <i>Amazon S3 User Guide</i>.
 *         If the upload was created using server-side encryption with Key Management Service (KMS) keys
 *         (SSE-KMS) or dual-layer server-side encryption with Amazon Web Services KMS keys (DSSE-KMS), you must
 *         have permission to the <code>kms:Decrypt</code> action for the <code>ListParts</code> request to
 *         succeed.
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
 *     HTTP Host header syntax
 *   </dt>
 *   <dd>
 *
 *     <p><b>Directory buckets </b> - The HTTP Host header syntax is <code><i>Bucket-name</i>.s3express-<i>zone-id</i>
 *     .<i>region-code</i>.amazonaws.com</code>.
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>ListParts</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html">AbortMultipartUpload</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObjectAttributes.html">GetObjectAttributes</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListMultipartUploads.html">ListMultipartUploads</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>To list parts of a multipart upload.</h3>
 *
 * <p>The following example lists parts uploaded for a specific multipart upload.{@snippet :
 * var input = ListPartsInput.builder()
 *                 .bucket("examplebucket").key("bigobject").uploadId("example7YPBOJuoFiQ9cz4P3Pe6FIZwO4f7wN93uHsNBEw97pl5eNwzExg0LAT2dUN91cOmrEQHDsP3WA60CEg--")
 *                 .build();
 *
 * var result = client.listParts(input);
 * result.equals(ListPartsOutput.builder()
 *                   .owner(Owner.builder()
 *                              .displayName("owner-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                              .build()).initiator(Initiator.builder()
 *                                  .displayName("owner-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                  .build()).parts(List.of(
 *                              Part.builder()
 *                                  .lastModified(Instant.parse("2016-12-16T00:11:42Z")).partNumber(1).eTag("\"d8c2eafd90c266e19ab9dcacc479f8af\"").size(26246026)
 *                                  .build()
 *                              ,
 *                              Part.builder()
 *                                  .lastModified(Instant.parse("2016-12-16T00:15:01Z")).partNumber(2).eTag("\"d8c2eafd90c266e19ab9dcacc479f8af\"").size(26246026)
 *                                  .build()
 *                          )).storageClass(StorageClass.STANDARD)
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class ListParts implements ApiOperation<ListPartsInput, ListPartsOutput> {

    private static final ListParts $INSTANCE = new ListParts();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#ListParts"),
            PaginatedTrait.builder().inputToken("PartNumberMarker").outputToken("NextPartNumberMarker").items("Parts").pageSize("MaxParts").build(),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?x-id=ListParts")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListParts instance() {
        return $INSTANCE;
    }

    private ListParts() {}

    @Override
    public ShapeBuilder<ListPartsInput> inputBuilder() {
        return ListPartsInput.builder();
    }

    @Override
    public ShapeBuilder<ListPartsOutput> outputBuilder() {
        return ListPartsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListPartsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListPartsOutput.$SCHEMA;
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
