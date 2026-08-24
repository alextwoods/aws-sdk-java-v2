package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.util.List;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * Returns the current provisioned-capacity quotas for your Amazon Web Services account in a Region, both for the Region
 * as a whole and for any one DynamoDB table that you create there.
 *
 * <p>When you establish an Amazon Web Services account, the account has initial quotas on the maximum read capacity
 * units and write capacity units that you can provision across all of your DynamoDB tables in a given Region. Also,
 * there are per-table quotas that apply when you create a table there. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html">Service, Account,
 * and Table Quotas</a> page in the <i>Amazon DynamoDB Developer Guide</i>.
 *
 * <p>Although you can increase these quotas by filing a case at <a href="https://console.aws.amazon.com/support/home#/">Amazon Web Services Support Center</a>, obtaining
 * the increase is not instantaneous. The <code>DescribeLimits</code> action lets you write code to compare the capacity
 * you are currently using to those quotas imposed by your account so that you have enough time to apply for an increase
 * before you hit a quota.
 *
 * <p>For example, you could use one of the Amazon Web Services SDKs to do the following:
 *
 * <ol>
 *   <li>
 *     Call <code>DescribeLimits</code> for a particular Region to obtain your current account quotas on provisioned
 *     capacity there.
 *   </li>
 *   <li>
 *     Create a variable to hold the aggregate read capacity units provisioned for all your tables in that Region,
 *     and one to hold the aggregate write capacity units. Zero them both.
 *   </li>
 *   <li>
 *     Call <code>ListTables</code> to obtain a list of all your DynamoDB tables.
 *   </li>
 *   <li>
 *     For each table name listed by <code>ListTables</code>, do the following:
 *
 *     <ul>
 *       <li>
 *         Call <code>DescribeTable</code> with the table name.
 *       </li>
 *       <li>
 *         Use the data returned by <code>DescribeTable</code> to add the read capacity units and write capacity
 *         units provisioned for the table itself to your variables.
 *       </li>
 *       <li>
 *         If the table has one or more global secondary indexes (GSIs), loop over these GSIs and add their
 *         provisioned capacity values to your variables as well.
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     Report the account quotas for that Region returned by <code>DescribeLimits</code>, along with the total
 *     current provisioned capacity levels you have calculated.
 *   </li>
 * </ol>
 *
 * <p>This will let you see whether you are getting close to your account-level quotas.
 *
 * <p>The per-table quotas apply only when you are creating a new table. They restrict the sum of the provisioned
 * capacity of the new table itself and all its global secondary indexes.
 *
 * <p>For existing tables and their GSIs, DynamoDB doesn't let you increase provisioned capacity extremely rapidly, but
 * the only quota that applies is that the aggregate provisioned capacity over all your tables and GSIs cannot exceed
 * either of the per-account quotas.
 *
 * <p><code>DescribeLimits</code> should only be called periodically. You can expect throttling errors if you call it
 * more than once in a minute.
 *
 * <p>The <code>DescribeLimits</code> Request element has no content.
 *
 * <h2>Examples</h2>
 * <h3>To determine capacity limits per table and account, in the current AWS region</h3>
 *
 * <p>The following example returns the maximum read and write capacity units per table, and for the AWS account, in the current AWS region.{@snippet :
 * var input = DescribeLimitsInput.builder()
 *
 *                 .build();
 *
 * var result = client.describeLimits(input);
 * result.equals(DescribeLimitsOutput.builder()
 *                   .tableMaxWriteCapacityUnits(10000).tableMaxReadCapacityUnits(10000).accountMaxReadCapacityUnits(20000).accountMaxWriteCapacityUnits(20000)
 *                   .build());
 * }
 *
 */
@SmithyGenerated
public final class DescribeLimits implements ApiOperation<DescribeLimitsInput, DescribeLimitsOutput> {

    private static final DescribeLimits $INSTANCE = new DescribeLimits();

    static final Schema $SCHEMA = Schema.createOperation(ShapeId.from("com.amazonaws.dynamodb#DescribeLimits"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.builder()
        .putType(InternalServerError.$ID, InternalServerError.class, InternalServerError::builder)
        .putType(InvalidEndpointException.$ID, InvalidEndpointException.class, InvalidEndpointException::builder)
        .build();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"));

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static DescribeLimits instance() {
        return $INSTANCE;
    }

    private DescribeLimits() {}

    @Override
    public ShapeBuilder<DescribeLimitsInput> inputBuilder() {
        return DescribeLimitsInput.builder();
    }

    @Override
    public ShapeBuilder<DescribeLimitsOutput> outputBuilder() {
        return DescribeLimitsOutput.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return DescribeLimitsInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return DescribeLimitsOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of(InternalServerError.$SCHEMA, InvalidEndpointException.$SCHEMA);
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
        return DynamoDBApiService.instance();
    }
    }
