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
 * Removes the customer managed Amazon Web Services Key Management Service (Amazon Web Services KMS) key association
 * from the specified dataset. After this operation completes, data that you publish to the dataset is encrypted at rest
 * using an Amazon Web Services owned key managed by Amazon CloudWatch.
 *
 * <p>Only the <code>default</code> dataset is supported. To call this operation, the dataset must currently have a
 * customer managed KMS key associated with it. If the dataset has no associated KMS key, the operation fails with <code>
 * ResourceNotFoundException</code>.
 *
 * <p>Amazon CloudWatch performs a dry-run <code>kms:Decrypt</code> call on the currently associated key as part of this
 * operation. The caller must have <code>kms:Decrypt</code> permission on the currently associated key. If the key is
 * accessible but the caller lacks <code>kms:Decrypt</code> permission, the operation fails with <code>
 * AccessDeniedException</code>.
 *
 * <p>If the currently associated key has been deleted, is scheduled for deletion, is pending import, is unavailable, or
 * has been disabled, Amazon CloudWatch does not require <code>kms:Decrypt</code> permission on that key and the
 * disassociation proceeds. If the key was only disabled, consider re-enabling it instead of disassociating, because
 * re-enabling allows Amazon CloudWatch to resume decrypting your existing metric data.
 *
 * <p>Disassociating a KMS key from a dataset does not immediately remove the <code>kms:Decrypt</code> requirement on
 * data plane operations. For up to three hours after disassociation, callers must continue to have <code>kms:Decrypt</code>
 * permission on the previously associated key. Some data might still be encrypted with that key during this window.
 * After this enforcement window elapses, the <code>kms:Decrypt</code> requirement is lifted.
 *
 * <p>For more information about using customer managed keys with Amazon CloudWatch, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cmk-encryption.html">Encryption at rest with
 * customer managed keys</a> in the <i>Amazon CloudWatch User Guide</i>.
 */
@SmithyGenerated
public final class DisassociateDatasetKmsKey implements ApiOperation<DisassociateDatasetKmsKeyInput, DisassociateDatasetKmsKeyOutput> {

    private static final DisassociateDatasetKmsKey $INSTANCE = new DisassociateDatasetKmsKey();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.cloudwatch#DisassociateDatasetKmsKey"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(ConflictException.$ID, ConflictException.class, ConflictException::builder)
        .putType(ResourceNotFoundException.$ID, ResourceNotFoundException.class, ResourceNotFoundException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DisassociateDatasetKmsKey instance() {
        return $INSTANCE;
    }

    private DisassociateDatasetKmsKey() {}

    @Override
    public ShapeBuilder<DisassociateDatasetKmsKeyInput> inputBuilder() {
        return DisassociateDatasetKmsKeyInput.builder();
    }

    @Override
    public ShapeBuilder<DisassociateDatasetKmsKeyOutput> outputBuilder() {
        return DisassociateDatasetKmsKeyOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DisassociateDatasetKmsKeyInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DisassociateDatasetKmsKeyOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(ConflictException.$SCHEMA, ResourceNotFoundException.$SCHEMA);
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
