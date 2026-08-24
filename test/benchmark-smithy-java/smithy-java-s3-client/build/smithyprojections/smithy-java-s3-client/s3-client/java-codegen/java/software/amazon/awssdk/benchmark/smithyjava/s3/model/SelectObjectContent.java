package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import java.util.function.Supplier;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>This action filters the contents of an Amazon S3 object based on a simple structured query language (SQL)
 * statement. In the request, along with the SQL expression, you must also specify a data serialization format (JSON,
 * CSV, or Apache Parquet) of the object. Amazon S3 uses this format to parse object data into records, and returns only
 * records that match the specified SQL expression. You must also specify the data serialization format for the
 * response.
 *
 * <p>This functionality is not supported for Amazon S3 on Outposts.
 *
 * <p>For more information about Amazon S3 Select, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/selecting-content-from-objects.html">Selecting Content from Objects</a> and <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/s3-glacier-select-sql-reference-select.html">SELECT Command</a>
 * in the <i>Amazon S3 User Guide</i>.
 *
 * <dl>
 *   <dt>
 *     Permissions
 *   </dt>
 *   <dd>
 *
 *     <p>You must have the <code>s3:GetObject</code> permission for this operation. Amazon S3 Select does not
 *     support anonymous access. For more information about permissions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/using-with-s3-actions.html">Specifying Permissions in a Policy</a>
 *     in the <i>Amazon S3 User Guide</i>.
 *   </dd>
 *   <dt>
 *     Object Data Formats
 *   </dt>
 *   <dd>
 *
 *     <p>You can use Amazon S3 Select to query objects that have the following format properties:
 *
 *     <ul>
 *       <li>
 *         <i>CSV, JSON, and Parquet</i> - Objects must be in CSV, JSON, or Parquet format.
 *       </li>
 *       <li>
 *         <i>UTF-8</i> - UTF-8 is the only encoding type Amazon S3 Select supports.
 *       </li>
 *       <li>
 *         <i>GZIP or BZIP2</i> - CSV and JSON files can be compressed using GZIP or BZIP2. GZIP and BZIP2 are
 *         the only compression formats that Amazon S3 Select supports for CSV and JSON files. Amazon S3 Select
 *         supports columnar compression for Parquet using GZIP or Snappy. Amazon S3 Select does not support
 *         whole-object compression for Parquet objects.
 *       </li>
 *       <li>
 *         <i>Server-side encryption</i> - Amazon S3 Select supports querying objects that are protected with
 *         server-side encryption.For objects that are encrypted with customer-provided encryption keys (SSE-C),
 *         you must use HTTPS, and you must use the headers that are documented in the <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>. For
 *         more information about SSE-C, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/ServerSideEncryptionCustomerKeys.html">Server-Side Encryption (Using Customer-Provided Encryption Keys)</a>
 *         in the <i>Amazon S3 User Guide</i>.For objects that are encrypted with Amazon S3 managed keys
 *         (SSE-S3) and Amazon Web Services KMS keys (SSE-KMS), server-side encryption is handled transparently,
 *         so you don't need to specify anything. For more information about server-side encryption, including
 *         SSE-S3 and SSE-KMS, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/serv-side-encryption.html">Protecting Data Using Server-Side Encryption</a> in the <i>Amazon S3 User
 *         Guide</i>.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Working with the Response Body
 *   </dt>
 *   <dd>
 *
 *     <p>Given the response size is unknown, Amazon S3 Select streams the response as a series of messages and
 *     includes a <code>Transfer-Encoding</code> header with <code>chunked</code> as its value in the response. For
 *     more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTSelectObjectAppendix.html">Appendix: SelectObjectContent Response</a>.
 *   </dd>
 *   <dt>
 *     GetObject Support
 *   </dt>
 *   <dd>
 *
 *     <p>The <code>SelectObjectContent</code> action does not support the following <code>GetObject</code>
 *     functionality. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>.
 *
 *     <ul>
 *       <li>
 *         <code>Range</code>: Although you can specify a scan range for an Amazon S3 Select request (see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_SelectObjectContent.html#AmazonS3-SelectObjectContent-request-ScanRange">
 *         SelectObjectContentRequest - ScanRange</a> in the request parameters), you cannot specify the range
 *         of bytes of an object to return.
 *       </li>
 *       <li>
 *         The <code>GLACIER</code>, <code>DEEP_ARCHIVE</code>, and <code>REDUCED_REDUNDANCY</code> storage
 *         classes, or the <code>ARCHIVE_ACCESS</code> and <code>DEEP_ARCHIVE_ACCESS</code> access tiers of the <code>
 *         INTELLIGENT_TIERING</code> storage class: You cannot query objects in the <code>GLACIER</code>, <code>
 *         DEEP_ARCHIVE</code>, or <code>REDUCED_REDUNDANCY</code> storage classes, nor objects in the <code>
 *         ARCHIVE_ACCESS</code> or <code>DEEP_ARCHIVE_ACCESS</code> access tiers of the <code>
 *         INTELLIGENT_TIERING</code> storage class. For more information about storage classes, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/storage-class-intro.html">Using
 *         Amazon S3 storage classes</a> in the <i>Amazon S3 User Guide</i>.
 *       </li>
 *     </ul>
 *   </dd>
 *   <dt>
 *     Special Errors
 *   </dt>
 *   <dd>
 *
 *     <p>For a list of special errors for this operation, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html#SelectObjectContentErrorCodeList">List of SELECT Object Content Error Codes</a>
 *   </dd>
 * </dl>
 *
 * <p>The following operations are related to <code>SelectObjectContent</code>:
 *
 * <ul>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetBucketLifecycleConfiguration.html">GetBucketLifecycleConfiguration</a>
 *   </li>
 *   <li>
 *     <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutBucketLifecycleConfiguration.html">PutBucketLifecycleConfiguration</a>
 *   </li>
 * </ul>
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class SelectObjectContent implements ApiOperation<SelectObjectContentInput, SelectObjectContentOutput> {

    private static final SelectObjectContent $INSTANCE = new SelectObjectContent();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#SelectObjectContent"),
            HttpTrait.builder().method("POST").code(200).uri(UriPattern.parse("/{Bucket}/{Key+}?select&select-type=2")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema OUTPUT_STREAM_MEMBER = SelectObjectContentOutput.$SCHEMA.member("Payload");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static SelectObjectContent instance() {
        return $INSTANCE;
    }

    private SelectObjectContent() {}

    @Override
    public ShapeBuilder<SelectObjectContentInput> inputBuilder() {
        return SelectObjectContentInput.builder();
    }

    @Override
    public ShapeBuilder<SelectObjectContentOutput> outputBuilder() {
        return SelectObjectContentOutput.builder();
    }

    @Override
    public Supplier<ShapeBuilder<? extends SerializableStruct>> outputEventBuilderSupplier() {
        return () -> SelectObjectContentEventStream.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return SelectObjectContentInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return SelectObjectContentOutput.$SCHEMA;
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
