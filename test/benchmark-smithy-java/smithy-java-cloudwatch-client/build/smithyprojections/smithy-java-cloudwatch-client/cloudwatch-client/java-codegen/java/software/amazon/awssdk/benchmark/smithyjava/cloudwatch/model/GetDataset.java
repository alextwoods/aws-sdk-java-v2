package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Returns information about the specified dataset. This includes its identifier, Amazon Resource Name (ARN), and any
 * customer managed Amazon Web Services Key Management Service (Amazon Web Services KMS) key that is currently
 * associated with it.
 *
 * <p>Only the <code>default</code> dataset is supported. The <code>default</code> dataset is implicit for every account
 * in every Region — you can call <code>GetDataset</code> for it without first creating it. If no customer managed KMS
 * key has been associated with the dataset, the response omits the <code>KmsKeyArn</code> field, indicating that data
 * is encrypted at rest using an Amazon Web Services owned key managed by Amazon CloudWatch.
 *
 * <p>To associate a customer managed KMS key with a dataset, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_AssociateDatasetKmsKey.html">AssociateDatasetKmsKey</a>. To remove the
 * association, use <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_DisassociateDatasetKmsKey.html">DisassociateDatasetKmsKey</a>.
 */
@SmithyGenerated
public final class GetDataset implements ApiOperation<GetDatasetInput, GetDatasetOutput> {

    private static final GetDataset $INSTANCE = new GetDataset();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#GetDataset"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static GetDataset instance() {
        return $INSTANCE;
    }

    private GetDataset() {}

    @Override
    public ShapeBuilder<GetDatasetInput> inputBuilder() {
        return GetDatasetInput.builder();
    }

    @Override
    public ShapeBuilder<GetDatasetOutput> outputBuilder() {
        return GetDatasetOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return GetDatasetInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return GetDatasetOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ResourceNotFoundException.$SCHEMA);
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
        return CloudWatchApiService.instance();
    }
    }
