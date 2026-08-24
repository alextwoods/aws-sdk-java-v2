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
 * This operation lists in-progress multipart uploads in a bucket. An in-progress multipart upload is a multipart upload
 * that has been initiated by the <code>CreateMultipartUpload</code> request, but has not yet been completed or aborted.
 *
 * <p><b>Directory buckets</b> - If multipart uploads in a directory bucket are in progress, you can't delete the bucket
 * until all the in-progress multipart uploads are aborted or completed. To delete these in-progress multipart uploads,
 * use the <code>ListMultipartUploads</code> operation to list the in-progress multipart uploads in the bucket and use
 * the <code>AbortMultipartUpload</code> operation to abort all the in-progress multipart uploads.
 *
 * <p>The <code>ListMultipartUploads</code> operation returns a maximum of 1,000 multipart uploads in the response. The
 * limit of 1,000 multipart uploads is also the default value. You can further limit the number of uploads in a response
 * by specifying the <code>max-uploads</code> request parameter. If there are more than 1,000 multipart uploads that
 * satisfy your <code>ListMultipartUploads</code> request, the response returns an <code>IsTruncated</code> element with
 * the value of <code>true</code>, a <code>NextKeyMarker</code> element, and a <code>NextUploadIdMarker</code> element.
 * To list the remaining multipart uploads, you need to make subsequent <code>ListMultipartUploads</code> requests. In
 * these requests, include two query parameters: <code>key-marker</code> and <code>upload-id-marker</code>. Set the
 * value of <code>key-marker</code> to the <code>NextKeyMarker</code> value from the previous response. Similarly, set
 * the value of <code>upload-id-marker</code> to the <code>NextUploadIdMarker</code> value from the previous response.
 *
 * <p><b>Directory buckets</b> - The <code>upload-id-marker</code> element and the <code>NextUploadIdMarker</code>
 * element aren't supported by directory buckets. To list the additional multipart uploads, you only need to set the
 * value of <code>key-marker</code> to the <code>NextKeyMarker</code> value from the previous response.
 *
 * <p>For more information about multipart uploads, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/uploadobjusingmpu.html">Uploading Objects Using Multipart Upload</a> in the <i>Amazon
 * S3 User Guide</i>.
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
 *     Sorting of multipart uploads in response
 *   </dt>
 *   <dd>
 *
 *     <ul>
 *       <li>
 *         <b>General purpose bucket</b> - In the <code>ListMultipartUploads</code> response, the multipart
 *         uploads are sorted based on two criteria:
 *
 *         <ul>
 *           <li>
 *             Key-based sorting - Multipart uploads are initially sorted in ascending order based on their
 *             object keys.
 *           </li>
 *           <li>
 *             Time-based sorting - For uploads that share the same object key, they are further sorted in
 *             ascending order based on the upload initiation time. Among uploads with the same key, the one
 *             that was initiated first will appear before the ones that were initiated later.
 *           </li>
 *         </ul>
 *       </li>
 *       <li>
 *         <b>Directory bucket</b> - In the <code>ListMultipartUploads</code> response, the multipart uploads
 *         aren't sorted lexicographically based on the object keys.
 *
 *         <pre>{@code
 *           </p>
 *               </li>
 *            </ul>
 *         </dd>
 *         <dt>HTTP Host header syntax</dt>
 *         <dd>
 *            <p>
 *               <b>Directory buckets </b> - The HTTP Host header syntax is <code>
 *                  <i>Bucket-name</i>.s3express-<i>zone-id</i>.<i>region-code</i>.amazonaws.com</code>.</p>
 *         </dd>
 *      </dl>
 *      <p>The following operations are related to <code>ListMultipartUploads</code>:</p>
 *      <ul>
 *         <li>
 *            <p>
 *               <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html">CreateMultipartUpload</a>
 *            </p>
 *         </li>
 *         <li>
 *            <p>
 *               <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html">UploadPart</a>
 *            </p>
 *         </li>
 *         <li>
 *            <p>
 *               <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html">CompleteMultipartUpload</a>
 *            </p>
 *         </li>
 *         <li>
 *            <p>
 *               <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html">ListParts</a>
 *            </p>
 *         </li>
 *         <li>
 *            <p>
 *               <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html">AbortMultipartUpload</a>
 *            </p>
 *         </li>
 *      </ul>
 *      <important>
 *         <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my  file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>my%20%20file.txt</code>.</p>
 *      </important>
 *
 *         }</pre>
 *       </li>
 *     </ul>
 *   </dd>
 * </dl>
 *
 * <h2>Examples</h2>
 * <h3>List next set of multipart uploads when previous result is truncated</h3>
 *
 * <p>The following example specifies the upload-id-marker and key-marker from previous truncated response to retrieve next setup of multipart uploads.{@snippet :
 * var input = ListMultipartUploadsInput.builder()
 *                 .bucket("examplebucket").keyMarker("nextkeyfrompreviousresponse").maxUploads(2).uploadIdMarker("valuefrompreviousresponse")
 *                 .build();
 *
 * var result = client.listMultipartUploads(input);
 * result.equals(ListMultipartUploadsOutput.builder()
 *                   .uploadIdMarker("").nextKeyMarker("someobjectkey").bucket("acl1").nextUploadIdMarker("examplelo91lv1iwvWpvCiJWugw2xXLPAD7Z8cJyX9.WiIRgNrdG6Ldsn.9FtS63TCl1Uf5faTB.1U5Ckcbmdw--").uploads(List.of(
 *                                MultipartUpload.builder()
 *                                    .initiator(Initiator.builder()
 *                                                   .displayName("ownder-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                   .build()).initiated(Instant.parse("2014-05-01T05:40:58Z")).uploadId("gZ30jIqlUa.CInXklLQtSMJITdUnoZ1Y5GACB5UckOtspm5zbDMCkPF_qkfZzMiFZ6dksmcnqxJyIBvQMG9X9Q--").storageClass(StorageClass.STANDARD).key("JavaFile").owner(Owner.builder()
 *                                               .displayName("mohanataws").id("852b113e7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                               .build())
 *                                    .build()
 *                                ,
 *                                MultipartUpload.builder()
 *                                    .initiator(Initiator.builder()
 *                                                   .displayName("ownder-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                   .build()).initiated(Instant.parse("2014-05-01T05:41:27Z")).uploadId("b7tZSqIlo91lv1iwvWpvCiJWugw2xXLPAD7Z8cJyX9.WiIRgNrdG6Ldsn.9FtS63TCl1Uf5faTB.1U5Ckcbmdw--").storageClass(StorageClass.STANDARD).key("JavaFile").owner(Owner.builder()
 *                                               .displayName("ownder-display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                               .build())
 *                                    .build()
 *                            )).keyMarker("").maxUploads(2).isTruncated(true)
 *                   .build());
 * }
 *
 * <h3>To list in-progress multipart uploads on a bucket</h3>
 *
 * <p>The following example lists in-progress multipart uploads on a specific bucket.{@snippet :
 * var input = ListMultipartUploadsInput.builder()
 *                 .bucket("examplebucket")
 *                 .build();
 *
 * var result = client.listMultipartUploads(input);
 * result.equals(ListMultipartUploadsOutput.builder()
 *                   .uploads(List.of(
 *                                MultipartUpload.builder()
 *                                    .initiator(Initiator.builder()
 *                                                   .displayName("display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                   .build()).initiated(Instant.parse("2014-05-01T05:40:58Z")).uploadId("examplelUa.CInXklLQtSMJITdUnoZ1Y5GACB5UckOtspm5zbDMCkPF_qkfZzMiFZ6dksmcnqxJyIBvQMG9X9Q--").storageClass(StorageClass.STANDARD).key("JavaFile").owner(Owner.builder()
 *                                               .displayName("display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                               .build())
 *                                    .build()
 *                                ,
 *                                MultipartUpload.builder()
 *                                    .initiator(Initiator.builder()
 *                                                   .displayName("display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                                   .build()).initiated(Instant.parse("2014-05-01T05:41:27Z")).uploadId("examplelo91lv1iwvWpvCiJWugw2xXLPAD7Z8cJyX9.WiIRgNrdG6Ldsn.9FtS63TCl1Uf5faTB.1U5Ckcbmdw--").storageClass(StorageClass.STANDARD).key("JavaFile").owner(Owner.builder()
 *                                               .displayName("display-name").id("examplee7a2f25102679df27bb0ae12b3f85be6f290b936c4393484be31bebcc")
 *                                               .build())
 *                                    .build()
 *                            ))
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class ListMultipartUploads implements ApiOperation<ListMultipartUploadsInput, ListMultipartUploadsOutput> {

    private static final ListMultipartUploads $INSTANCE = new ListMultipartUploads();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#ListMultipartUploads"),
            HttpTrait.builder().method("GET").code(200).uri(UriPattern.parse("/{Bucket}?uploads")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static ListMultipartUploads instance() {
        return $INSTANCE;
    }

    private ListMultipartUploads() {}

    @Override
    public ShapeBuilder<ListMultipartUploadsInput> inputBuilder() {
        return ListMultipartUploadsInput.builder();
    }

    @Override
    public ShapeBuilder<ListMultipartUploadsOutput> outputBuilder() {
        return ListMultipartUploadsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return ListMultipartUploadsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return ListMultipartUploadsOutput.$SCHEMA;
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
