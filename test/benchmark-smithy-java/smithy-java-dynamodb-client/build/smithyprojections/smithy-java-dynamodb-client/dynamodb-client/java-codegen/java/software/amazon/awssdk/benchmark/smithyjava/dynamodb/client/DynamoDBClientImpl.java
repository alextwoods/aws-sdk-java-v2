package software.amazon.awssdk.benchmark.smithyjava.dynamodb.client;

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchExecuteStatement;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchExecuteStatementInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchExecuteStatementOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchGetItem;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchGetItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchGetItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchWriteItem;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchWriteItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchWriteItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateBackup;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateBackupInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateBackupOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateGlobalTable;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateGlobalTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateGlobalTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateTable;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.CreateTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteBackup;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteBackupInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteBackupOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteItem;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteResourcePolicy;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteResourcePolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteResourcePolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteTable;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DeleteTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeBackup;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeBackupInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeBackupOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContinuousBackups;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContinuousBackupsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContinuousBackupsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContributorInsights;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContributorInsightsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeContributorInsightsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeEndpoints;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeEndpointsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeEndpointsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeExport;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeExportInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeExportOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTable;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableSettings;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableSettingsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeGlobalTableSettingsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeImport;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeImportInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeImportOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeKinesisStreamingDestination;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeLimits;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeLimitsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeLimitsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTable;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableReplicaAutoScaling;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableReplicaAutoScalingInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTableReplicaAutoScalingOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTimeToLive;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTimeToLiveInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DescribeTimeToLiveOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DisableKinesisStreamingDestination;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DisableKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.DisableKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.EnableKinesisStreamingDestination;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.EnableKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.EnableKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteStatement;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteStatementInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteStatementOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteTransaction;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteTransactionInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExecuteTransactionOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExportTableToPointInTime;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExportTableToPointInTimeInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ExportTableToPointInTimeOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItem;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetResourcePolicy;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetResourcePolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetResourcePolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ImportTable;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ImportTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ImportTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListBackups;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListBackupsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListBackupsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListContributorInsights;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListContributorInsightsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListContributorInsightsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListExports;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListExportsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListExportsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListGlobalTables;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListGlobalTablesInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListGlobalTablesOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListImports;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListImportsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListImportsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTables;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTablesInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTablesOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTagsOfResource;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTagsOfResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ListTagsOfResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItem;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutResourcePolicy;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutResourcePolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutResourcePolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.Query;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.QueryInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.QueryOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableFromBackup;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableFromBackupInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableFromBackupOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableToPointInTime;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableToPointInTimeInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.RestoreTableToPointInTimeOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.Scan;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ScanInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.ScanOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.SearchVectors;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.SearchVectorsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.SearchVectorsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TagResource;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TagResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TagResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactGetItems;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactGetItemsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactGetItemsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactWriteItems;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactWriteItemsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.TransactWriteItemsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UntagResource;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UntagResourceInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UntagResourceOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContinuousBackups;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContinuousBackupsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContinuousBackupsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContributorInsights;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContributorInsightsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateContributorInsightsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTable;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableSettings;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableSettingsInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateGlobalTableSettingsOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateItem;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateItemInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateItemOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateKinesisStreamingDestination;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateKinesisStreamingDestinationInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateKinesisStreamingDestinationOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTable;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableReplicaAutoScaling;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableReplicaAutoScalingInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTableReplicaAutoScalingOutput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTimeToLive;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTimeToLiveInput;
import software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.UpdateTimeToLiveOutput;
import software.amazon.smithy.java.client.core.Client;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.core.VersionCheck;
import software.amazon.smithy.java.versionspi.ModuleVersion;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
final class DynamoDBClientImpl extends Client implements DynamoDBClient {

    private static final ModuleVersion CODEGEN_VERSION = new ModuleVersion("codegen", 1, 5, 1);

    DynamoDBClientImpl(DynamoDBClient.Builder builder) {
        super(builder);
        VersionCheck.check(CODEGEN_VERSION);
    }

    @Override
    public BatchExecuteStatementOutput batchExecuteStatement(BatchExecuteStatementInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, BatchExecuteStatement.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public BatchGetItemOutput batchGetItem(BatchGetItemInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, BatchGetItem.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public BatchWriteItemOutput batchWriteItem(BatchWriteItemInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, BatchWriteItem.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CreateBackupOutput createBackup(CreateBackupInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CreateBackup.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CreateGlobalTableOutput createGlobalTable(CreateGlobalTableInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CreateGlobalTable.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CreateTableOutput createTable(CreateTableInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CreateTable.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBackupOutput deleteBackup(DeleteBackupInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBackup.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteItemOutput deleteItem(DeleteItemInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteItem.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteResourcePolicyOutput deleteResourcePolicy(DeleteResourcePolicyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteResourcePolicy.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteTableOutput deleteTable(DeleteTableInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteTable.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeBackupOutput describeBackup(DescribeBackupInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeBackup.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeContinuousBackupsOutput describeContinuousBackups(DescribeContinuousBackupsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeContinuousBackups.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeContributorInsightsOutput describeContributorInsights(DescribeContributorInsightsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeContributorInsights.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeEndpointsOutput describeEndpoints(DescribeEndpointsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeEndpoints.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeExportOutput describeExport(DescribeExportInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeExport.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeGlobalTableOutput describeGlobalTable(DescribeGlobalTableInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeGlobalTable.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeGlobalTableSettingsOutput describeGlobalTableSettings(DescribeGlobalTableSettingsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeGlobalTableSettings.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeImportOutput describeImport(DescribeImportInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeImport.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeKinesisStreamingDestinationOutput describeKinesisStreamingDestination(DescribeKinesisStreamingDestinationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeKinesisStreamingDestination.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeLimitsOutput describeLimits(DescribeLimitsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeLimits.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeTableOutput describeTable(DescribeTableInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeTable.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeTableReplicaAutoScalingOutput describeTableReplicaAutoScaling(DescribeTableReplicaAutoScalingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeTableReplicaAutoScaling.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DescribeTimeToLiveOutput describeTimeToLive(DescribeTimeToLiveInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DescribeTimeToLive.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DisableKinesisStreamingDestinationOutput disableKinesisStreamingDestination(DisableKinesisStreamingDestinationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DisableKinesisStreamingDestination.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public EnableKinesisStreamingDestinationOutput enableKinesisStreamingDestination(EnableKinesisStreamingDestinationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, EnableKinesisStreamingDestination.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ExecuteStatementOutput executeStatement(ExecuteStatementInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ExecuteStatement.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ExecuteTransactionOutput executeTransaction(ExecuteTransactionInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ExecuteTransaction.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ExportTableToPointInTimeOutput exportTableToPointInTime(ExportTableToPointInTimeInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ExportTableToPointInTime.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetItemOutput getItem(GetItemInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetItem.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetResourcePolicyOutput getResourcePolicy(GetResourcePolicyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetResourcePolicy.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ImportTableOutput importTable(ImportTableInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ImportTable.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListBackupsOutput listBackups(ListBackupsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListBackups.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListContributorInsightsOutput listContributorInsights(ListContributorInsightsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListContributorInsights.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListExportsOutput listExports(ListExportsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListExports.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListGlobalTablesOutput listGlobalTables(ListGlobalTablesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListGlobalTables.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListImportsOutput listImports(ListImportsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListImports.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListTablesOutput listTables(ListTablesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListTables.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListTagsOfResourceOutput listTagsOfResource(ListTagsOfResourceInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListTagsOfResource.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutItemOutput putItem(PutItemInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutItem.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutResourcePolicyOutput putResourcePolicy(PutResourcePolicyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutResourcePolicy.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public QueryOutput query(QueryInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, Query.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public RestoreTableFromBackupOutput restoreTableFromBackup(RestoreTableFromBackupInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, RestoreTableFromBackup.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public RestoreTableToPointInTimeOutput restoreTableToPointInTime(RestoreTableToPointInTimeInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, RestoreTableToPointInTime.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ScanOutput scan(ScanInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, Scan.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public SearchVectorsOutput searchVectors(SearchVectorsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, SearchVectors.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public TagResourceOutput tagResource(TagResourceInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, TagResource.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public TransactGetItemsOutput transactGetItems(TransactGetItemsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, TransactGetItems.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public TransactWriteItemsOutput transactWriteItems(TransactWriteItemsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, TransactWriteItems.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UntagResourceOutput untagResource(UntagResourceInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UntagResource.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateContinuousBackupsOutput updateContinuousBackups(UpdateContinuousBackupsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateContinuousBackups.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateContributorInsightsOutput updateContributorInsights(UpdateContributorInsightsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateContributorInsights.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateGlobalTableOutput updateGlobalTable(UpdateGlobalTableInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateGlobalTable.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateGlobalTableSettingsOutput updateGlobalTableSettings(UpdateGlobalTableSettingsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateGlobalTableSettings.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateItemOutput updateItem(UpdateItemInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateItem.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateKinesisStreamingDestinationOutput updateKinesisStreamingDestination(UpdateKinesisStreamingDestinationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateKinesisStreamingDestination.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateTableOutput updateTable(UpdateTableInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateTable.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateTableReplicaAutoScalingOutput updateTableReplicaAutoScaling(UpdateTableReplicaAutoScalingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateTableReplicaAutoScaling.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateTimeToLiveOutput updateTimeToLive(UpdateTimeToLiveInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateTimeToLive.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DynamoDBWaiter waiter() {
        return new DynamoDBWaiter(this);
    }

}
