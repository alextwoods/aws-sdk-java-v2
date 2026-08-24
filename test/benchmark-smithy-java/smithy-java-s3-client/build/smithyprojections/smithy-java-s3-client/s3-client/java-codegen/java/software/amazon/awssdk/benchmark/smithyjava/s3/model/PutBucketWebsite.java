package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * This operation is not supported for directory buckets.
 *
 * <p>Sets the configuration of the website that is specified in the <code>website</code> subresource. To configure a
 * bucket as a website, you can add this subresource on the bucket with website configuration information such as the
 * file name of the index document and any redirect rules. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/WebsiteHosting.html">Hosting Websites on Amazon S3</a>
 * .
 *
 * <p>This PUT action requires the <code>S3:PutBucketWebsite</code> permission. By default, only the bucket owner can
 * configure the website attached to a bucket; however, bucket owners can allow other users to set the website
 * configuration by writing a bucket policy that grants them the <code>S3:PutBucketWebsite</code> permission.
 *
 * <p>To redirect all website requests sent to the bucket's website endpoint, you add a website configuration with the
 * following elements. Because all requests are sent to another website, you don't need to provide index document name
 * for the bucket.
 *
 * <ul>
 *   <li>
 *     <code>WebsiteConfiguration</code>
 *   </li>
 *   <li>
 *     <code>RedirectAllRequestsTo</code>
 *   </li>
 *   <li>
 *     <code>HostName</code>
 *   </li>
 *   <li>
 *     <code>Protocol</code>
 *   </li>
 * </ul>
 *
 * <p>If you want granular control over redirects, you can use the following elements to add routing rules that describe
 * conditions for redirecting requests and information about the redirect destination. In this case, the website
 * configuration must provide an index document for the bucket, because some requests might not be redirected.
 *
 * <ul>
 *   <li>
 *     <code>WebsiteConfiguration</code>
 *   </li>
 *   <li>
 *     <code>IndexDocument</code>
 *   </li>
 *   <li>
 *     <code>Suffix</code>
 *   </li>
 *   <li>
 *     <code>ErrorDocument</code>
 *   </li>
 *   <li>
 *     <code>Key</code>
 *   </li>
 *   <li>
 *     <code>RoutingRules</code>
 *   </li>
 *   <li>
 *     <code>RoutingRule</code>
 *   </li>
 *   <li>
 *     <code>Condition</code>
 *   </li>
 *   <li>
 *     <code>HttpErrorCodeReturnedEquals</code>
 *   </li>
 *   <li>
 *     <code>KeyPrefixEquals</code>
 *   </li>
 *   <li>
 *     <code>Redirect</code>
 *   </li>
 *   <li>
 *     <code>Protocol</code>
 *   </li>
 *   <li>
 *     <code>HostName</code>
 *   </li>
 *   <li>
 *     <code>ReplaceKeyPrefixWith</code>
 *   </li>
 *   <li>
 *     <code>ReplaceKeyWith</code>
 *   </li>
 *   <li>
 *     <code>HttpRedirectCode</code>
 *   </li>
 * </ul>
 *
 * <p>Amazon S3 has a limitation of 50 routing rules per website configuration. If you require more than 50 routing
 * rules, you can use object redirect. For more information, see <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/how-to-page-redirect.html">Configuring an Object Redirect</a> in the <i>Amazon
 * S3 User Guide</i>.
 *
 * <p>The maximum request length is limited to 128 KB.
 *
 * <p>You must URL encode any signed header values that contain spaces. For example, if your header value is <code>my
 * file.txt</code>, containing two spaces after <code>my</code>, you must URL encode this value to <code>
 * my%20%20file.txt</code>.
 *
 * <h2>Examples</h2>
 * <h3>Set website configuration on a bucket</h3>
 *
 * <p>The following example adds website configuration to a bucket.{@snippet :
 * var input = PutBucketWebsiteInput.builder()
 *                 .bucket("examplebucket").contentmD5("").websiteConfiguration(WebsiteConfiguration.builder()
 *                                           .indexDocument(IndexDocument.builder()
 *                                                              .suffix("index.html")
 *                                                              .build()).errorDocument(ErrorDocument.builder()
 *                                                              .key("error.html")
 *                                                              .build())
 *                                           .build())
 *                 .build();
 *
 * var result = client.putBucketWebsite(input);
 * result.equals();
 * }
 *
 */
@SmithyGenerated
public final class PutBucketWebsite implements ApiOperation<PutBucketWebsiteInput, PutBucketWebsiteOutput> {

    private static final PutBucketWebsite $INSTANCE = new PutBucketWebsite();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.s3#PutBucketWebsite"),
            new HttpChecksumTrait.Provider().createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                Node.objectNodeBuilder()
                    .withMember("requestAlgorithmMember", "ChecksumAlgorithm")
                    .withMember("requestChecksumRequired", true)
                    .build()
            ),
            new StaticContextParamsTrait.Provider().createTrait(
                ShapeId.from("smithy.rules#staticContextParams"),
                Node.objectNodeBuilder()
                    .withMember("UseS3ExpressControlEndpoint", Node.objectNodeBuilder()
                        .withMember("value", true)
                        .build())
                    .build()
            ),
            HttpTrait.builder().method("PUT").code(200).uri(UriPattern.parse("/{Bucket}?website")).build());

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static PutBucketWebsite instance() {
        return $INSTANCE;
    }

    private PutBucketWebsite() {}

    @Override
    public ShapeBuilder<PutBucketWebsiteInput> inputBuilder() {
        return PutBucketWebsiteInput.builder();
    }

    @Override
    public ShapeBuilder<PutBucketWebsiteOutput> outputBuilder() {
        return PutBucketWebsiteOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return PutBucketWebsiteInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return PutBucketWebsiteOutput.$SCHEMA;
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
