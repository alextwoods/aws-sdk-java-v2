package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Passes transformed objects to a <code>GetObject</code> operation when using Object Lambda access points. For
 * information about Object Lambda access points, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/transforming-objects.html">Transforming objects with Object Lambda access points</a> in
 * the <i>Amazon S3 User Guide</i>.
 *
 * <p>This operation supports metadata that can be returned by <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html">GetObject</a>, in addition to <code>RequestRoute</code>
 * , <code>RequestToken</code>, <code>StatusCode</code>, <code>ErrorCode</code>, and <code>ErrorMessage</code>. The <code>
 * GetObject</code> response metadata is supported so that the <code>WriteGetObjectResponse</code> caller, typically an
 * Lambda function, can provide the same metadata when it internally invokes <code>GetObject</code>. When <code>
 * WriteGetObjectResponse</code> is called by a customer-owned Lambda function, the metadata returned to the end user <code>
 * GetObject</code> call might differ from what Amazon S3 would normally return.
 *
 * <p>You can include any number of metadata headers. When including a metadata header, it should be prefaced with <code>
 * x-amz-meta</code>. For example, <code>x-amz-meta-my-custom-header: MyCustomValue</code>. The primary use case for
 * this is to forward <code>GetObject</code> metadata.
 *
 * <p>Amazon Web Services provides some prebuilt Lambda functions that you can use with S3 Object Lambda to detect and
 * redact personally identifiable information (PII) and decompress S3 objects. These Lambda functions are available in
 * the Amazon Web Services Serverless Application Repository, and can be selected through the Amazon Web Services
 * Management Console when you create your Object Lambda access point.
 *
 * <p>Example 1: PII Access Control - This Lambda function uses Amazon Comprehend, a natural language processing (NLP)
 * service using machine learning to find insights and relationships in text. It automatically detects personally
 * identifiable information (PII) such as names, addresses, dates, credit card numbers, and social security numbers from
 * documents in your Amazon S3 bucket.
 *
 * <p>Example 2: PII Redaction - This Lambda function uses Amazon Comprehend, a natural language processing (NLP)
 * service using machine learning to find insights and relationships in text. It automatically redacts personally
 * identifiable information (PII) such as names, addresses, dates, credit card numbers, and social security numbers from
 * documents in your Amazon S3 bucket.
 *
 * <p>Example 3: Decompression - The Lambda function S3ObjectLambdaDecompression, is equipped to decompress objects
 * stored in S3 in one of six compressed file formats including bzip2, gzip, snappy, zlib, zstandard and ZIP.
 *
 * <p>For information on how to view and use these functions, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/olap-examples.html">Using Amazon Web Services built Lambda functions</a>
 * in the <i>Amazon S3 User Guide</i>.
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 */
@SmithyGenerated
public final class WriteGetObjectResponse implements ApiOperation<WriteGetObjectResponseInput, WriteGetObjectResponseOutput> {

    private static final WriteGetObjectResponse $INSTANCE = new WriteGetObjectResponse();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#WriteGetObjectResponse"),
            new UnsignedPayloadTrait(),
            EndpointTrait.builder().hostPrefix("{RequestRoute}.").build(),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseObjectLambdaEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("POST").code(200).uri(UriPattern.parse("/WriteGetObjectResponse")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    private static final Schema INPUT_STREAM_MEMBER = WriteGetObjectResponseInput.$SCHEMA.member("Body");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static WriteGetObjectResponse instance() {
        return $INSTANCE;
    }

    private WriteGetObjectResponse() {}

    @Override
    public ShapeBuilder<WriteGetObjectResponseInput> inputBuilder() {
        return WriteGetObjectResponseInput.builder();
    }

    @Override
    public ShapeBuilder<WriteGetObjectResponseOutput> outputBuilder() {
        return WriteGetObjectResponseOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return WriteGetObjectResponseInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return WriteGetObjectResponseOutput.$SCHEMA;
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
        return INPUT_STREAM_MEMBER;
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
