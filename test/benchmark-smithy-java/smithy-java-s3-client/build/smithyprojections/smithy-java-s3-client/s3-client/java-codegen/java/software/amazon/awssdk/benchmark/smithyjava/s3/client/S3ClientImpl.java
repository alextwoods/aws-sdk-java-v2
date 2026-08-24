package software.amazon.awssdk.benchmark.smithyjava.s3.client;

import java.util.concurrent.CompletionException;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.AbortMultipartUpload;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.AbortMultipartUploadInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.AbortMultipartUploadOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CompleteMultipartUpload;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CompleteMultipartUploadInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CompleteMultipartUploadOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CopyObject;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CopyObjectInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CopyObjectOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucket;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucketInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucketMetadataConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucketMetadataConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucketMetadataConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucketMetadataTableConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucketMetadataTableConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucketMetadataTableConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateBucketOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateMultipartUpload;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateMultipartUploadInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateMultipartUploadOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateSession;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateSessionInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.CreateSessionOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucket;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketAnalyticsConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketAnalyticsConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketAnalyticsConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketCors;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketCorsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketCorsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketEncryption;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketEncryptionInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketEncryptionOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketIntelligentTieringConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketIntelligentTieringConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketIntelligentTieringConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketInventoryConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketInventoryConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketInventoryConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketLifecycle;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketLifecycleInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketLifecycleOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetadataConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetadataConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetadataConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetadataTableConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetadataTableConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetadataTableConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetricsConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetricsConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketMetricsConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketOwnershipControls;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketOwnershipControlsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketOwnershipControlsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketPolicy;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketPolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketPolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketReplication;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketReplicationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketReplicationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketTagging;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketTaggingInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketTaggingOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketWebsite;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketWebsiteInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteBucketWebsiteOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObject;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectAnnotation;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectAnnotationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectAnnotationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectTagging;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectTaggingInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectTaggingOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjects;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeleteObjectsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeletePublicAccessBlock;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeletePublicAccessBlockInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.DeletePublicAccessBlockOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAbac;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAbacInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAbacOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAccelerateConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAccelerateConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAccelerateConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAcl;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAclInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAclOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAnalyticsConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAnalyticsConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketAnalyticsConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketCors;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketCorsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketCorsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketEncryption;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketEncryptionInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketEncryptionOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketIntelligentTieringConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketIntelligentTieringConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketIntelligentTieringConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketInventoryConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketInventoryConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketInventoryConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLifecycleConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLifecycleConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLifecycleConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLocation;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLocationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLocationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLogging;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLoggingInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketLoggingOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetadataConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetadataConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetadataConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetadataTableConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetadataTableConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetadataTableConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetricsConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetricsConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketMetricsConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketNotificationConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketNotificationConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketNotificationConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketOwnershipControls;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketOwnershipControlsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketOwnershipControlsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketPolicy;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketPolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketPolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketPolicyStatus;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketPolicyStatusInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketPolicyStatusOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketReplication;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketReplicationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketReplicationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketRequestPayment;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketRequestPaymentInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketRequestPaymentOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketTagging;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketTaggingInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketTaggingOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketVersioning;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketVersioningInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketVersioningOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketWebsite;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketWebsiteInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetBucketWebsiteOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObject;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAcl;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAclInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAclOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAnnotation;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAnnotationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAnnotationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAttributes;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAttributesInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectAttributesOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectLegalHold;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectLegalHoldInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectLegalHoldOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectLockConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectLockConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectLockConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectRetention;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectRetentionInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectRetentionOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectTagging;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectTaggingInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectTaggingOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectTorrent;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectTorrentInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectTorrentOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetPublicAccessBlock;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetPublicAccessBlockInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.GetPublicAccessBlockOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadBucket;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadBucketInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadBucketOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadObject;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadObjectInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.HeadObjectOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketAnalyticsConfigurations;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketAnalyticsConfigurationsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketAnalyticsConfigurationsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketIntelligentTieringConfigurations;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketIntelligentTieringConfigurationsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketIntelligentTieringConfigurationsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketInventoryConfigurations;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketInventoryConfigurationsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketInventoryConfigurationsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketMetricsConfigurations;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketMetricsConfigurationsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketMetricsConfigurationsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBuckets;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListBucketsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListDirectoryBuckets;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListDirectoryBucketsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListDirectoryBucketsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListMultipartUploads;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListMultipartUploadsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListMultipartUploadsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectAnnotations;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectAnnotationsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectAnnotationsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectVersions;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectVersionsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectVersionsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjects;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectsV2;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectsV2Input;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListObjectsV2Output;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListParts;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListPartsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.ListPartsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAbac;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAbacInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAbacOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAccelerateConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAccelerateConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAccelerateConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAcl;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAclInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAclOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAnalyticsConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAnalyticsConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketAnalyticsConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketCors;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketCorsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketCorsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketEncryption;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketEncryptionInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketEncryptionOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketIntelligentTieringConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketIntelligentTieringConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketIntelligentTieringConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketInventoryConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketInventoryConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketInventoryConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketLifecycleConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketLifecycleConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketLifecycleConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketLogging;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketLoggingInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketLoggingOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketMetricsConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketMetricsConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketMetricsConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketNotificationConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketNotificationConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketNotificationConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketOwnershipControls;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketOwnershipControlsInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketOwnershipControlsOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketPolicy;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketPolicyInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketPolicyOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketReplication;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketReplicationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketReplicationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketRequestPayment;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketRequestPaymentInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketRequestPaymentOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketTagging;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketTaggingInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketTaggingOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketVersioning;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketVersioningInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketVersioningOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketWebsite;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketWebsiteInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutBucketWebsiteOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObject;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectAcl;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectAclInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectAclOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectAnnotation;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectAnnotationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectAnnotationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectLegalHold;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectLegalHoldInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectLegalHoldOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectLockConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectLockConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectLockConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectRetention;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectRetentionInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectRetentionOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectTagging;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectTaggingInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectTaggingOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutPublicAccessBlock;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutPublicAccessBlockInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.PutPublicAccessBlockOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.RenameObject;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.RenameObjectInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.RenameObjectOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.RestoreObject;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.RestoreObjectInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.RestoreObjectOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.SelectObjectContent;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.SelectObjectContentInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.SelectObjectContentOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataAnnotationTableConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataAnnotationTableConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataAnnotationTableConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataInventoryTableConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataInventoryTableConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataInventoryTableConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataJournalTableConfiguration;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataJournalTableConfigurationInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateBucketMetadataJournalTableConfigurationOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateObjectEncryption;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateObjectEncryptionInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UpdateObjectEncryptionOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UploadPart;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UploadPartCopy;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UploadPartCopyInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UploadPartCopyOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UploadPartInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.UploadPartOutput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.WriteGetObjectResponse;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.WriteGetObjectResponseInput;
import software.amazon.awssdk.benchmark.smithyjava.s3.model.WriteGetObjectResponseOutput;
import software.amazon.smithy.java.client.core.Client;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.core.VersionCheck;
import software.amazon.smithy.java.versionspi.ModuleVersion;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
final class S3ClientImpl extends Client implements S3Client {

    private static final ModuleVersion CODEGEN_VERSION = new ModuleVersion("codegen", 1, 5, 1);

    S3ClientImpl(S3Client.Builder builder) {
        super(builder);
        VersionCheck.check(CODEGEN_VERSION);
    }

    @Override
    public AbortMultipartUploadOutput abortMultipartUpload(AbortMultipartUploadInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, AbortMultipartUpload.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CompleteMultipartUploadOutput completeMultipartUpload(CompleteMultipartUploadInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CompleteMultipartUpload.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CopyObjectOutput copyObject(CopyObjectInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CopyObject.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CreateBucketOutput createBucket(CreateBucketInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CreateBucket.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CreateBucketMetadataConfigurationOutput createBucketMetadataConfiguration(CreateBucketMetadataConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CreateBucketMetadataConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CreateBucketMetadataTableConfigurationOutput createBucketMetadataTableConfiguration(CreateBucketMetadataTableConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CreateBucketMetadataTableConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CreateMultipartUploadOutput createMultipartUpload(CreateMultipartUploadInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CreateMultipartUpload.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public CreateSessionOutput createSession(CreateSessionInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, CreateSession.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketOutput deleteBucket(DeleteBucketInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucket.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketAnalyticsConfigurationOutput deleteBucketAnalyticsConfiguration(DeleteBucketAnalyticsConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketAnalyticsConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketCorsOutput deleteBucketCors(DeleteBucketCorsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketCors.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketEncryptionOutput deleteBucketEncryption(DeleteBucketEncryptionInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketEncryption.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketIntelligentTieringConfigurationOutput deleteBucketIntelligentTieringConfiguration(DeleteBucketIntelligentTieringConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketIntelligentTieringConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketInventoryConfigurationOutput deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketInventoryConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketLifecycleOutput deleteBucketLifecycle(DeleteBucketLifecycleInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketLifecycle.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketMetadataConfigurationOutput deleteBucketMetadataConfiguration(DeleteBucketMetadataConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketMetadataConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketMetadataTableConfigurationOutput deleteBucketMetadataTableConfiguration(DeleteBucketMetadataTableConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketMetadataTableConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketMetricsConfigurationOutput deleteBucketMetricsConfiguration(DeleteBucketMetricsConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketMetricsConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketOwnershipControlsOutput deleteBucketOwnershipControls(DeleteBucketOwnershipControlsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketOwnershipControls.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketPolicyOutput deleteBucketPolicy(DeleteBucketPolicyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketPolicy.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketReplicationOutput deleteBucketReplication(DeleteBucketReplicationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketReplication.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketTaggingOutput deleteBucketTagging(DeleteBucketTaggingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketTagging.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteBucketWebsiteOutput deleteBucketWebsite(DeleteBucketWebsiteInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteBucketWebsite.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteObjectOutput deleteObject(DeleteObjectInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteObject.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteObjectAnnotationOutput deleteObjectAnnotation(DeleteObjectAnnotationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteObjectAnnotation.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteObjectsOutput deleteObjects(DeleteObjectsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteObjects.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeleteObjectTaggingOutput deleteObjectTagging(DeleteObjectTaggingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeleteObjectTagging.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public DeletePublicAccessBlockOutput deletePublicAccessBlock(DeletePublicAccessBlockInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, DeletePublicAccessBlock.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketAbacOutput getBucketAbac(GetBucketAbacInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketAbac.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketAccelerateConfigurationOutput getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketAccelerateConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketAclOutput getBucketAcl(GetBucketAclInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketAcl.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketAnalyticsConfigurationOutput getBucketAnalyticsConfiguration(GetBucketAnalyticsConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketAnalyticsConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketCorsOutput getBucketCors(GetBucketCorsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketCors.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketEncryptionOutput getBucketEncryption(GetBucketEncryptionInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketEncryption.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketIntelligentTieringConfigurationOutput getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketIntelligentTieringConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketInventoryConfigurationOutput getBucketInventoryConfiguration(GetBucketInventoryConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketInventoryConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketLifecycleConfigurationOutput getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketLifecycleConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketLocationOutput getBucketLocation(GetBucketLocationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketLocation.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketLoggingOutput getBucketLogging(GetBucketLoggingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketLogging.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketMetadataConfigurationOutput getBucketMetadataConfiguration(GetBucketMetadataConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketMetadataConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketMetadataTableConfigurationOutput getBucketMetadataTableConfiguration(GetBucketMetadataTableConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketMetadataTableConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketMetricsConfigurationOutput getBucketMetricsConfiguration(GetBucketMetricsConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketMetricsConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketNotificationConfigurationOutput getBucketNotificationConfiguration(GetBucketNotificationConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketNotificationConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketOwnershipControlsOutput getBucketOwnershipControls(GetBucketOwnershipControlsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketOwnershipControls.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketPolicyOutput getBucketPolicy(GetBucketPolicyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketPolicy.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketPolicyStatusOutput getBucketPolicyStatus(GetBucketPolicyStatusInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketPolicyStatus.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketReplicationOutput getBucketReplication(GetBucketReplicationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketReplication.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketRequestPaymentOutput getBucketRequestPayment(GetBucketRequestPaymentInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketRequestPayment.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketTaggingOutput getBucketTagging(GetBucketTaggingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketTagging.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketVersioningOutput getBucketVersioning(GetBucketVersioningInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketVersioning.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetBucketWebsiteOutput getBucketWebsite(GetBucketWebsiteInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetBucketWebsite.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectOutput getObject(GetObjectInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObject.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectAclOutput getObjectAcl(GetObjectAclInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObjectAcl.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectAnnotationOutput getObjectAnnotation(GetObjectAnnotationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObjectAnnotation.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectAttributesOutput getObjectAttributes(GetObjectAttributesInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObjectAttributes.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectLegalHoldOutput getObjectLegalHold(GetObjectLegalHoldInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObjectLegalHold.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectLockConfigurationOutput getObjectLockConfiguration(GetObjectLockConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObjectLockConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectRetentionOutput getObjectRetention(GetObjectRetentionInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObjectRetention.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectTaggingOutput getObjectTagging(GetObjectTaggingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObjectTagging.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetObjectTorrentOutput getObjectTorrent(GetObjectTorrentInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetObjectTorrent.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public GetPublicAccessBlockOutput getPublicAccessBlock(GetPublicAccessBlockInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, GetPublicAccessBlock.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public HeadBucketOutput headBucket(HeadBucketInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, HeadBucket.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public HeadObjectOutput headObject(HeadObjectInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, HeadObject.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListBucketAnalyticsConfigurationsOutput listBucketAnalyticsConfigurations(ListBucketAnalyticsConfigurationsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListBucketAnalyticsConfigurations.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListBucketIntelligentTieringConfigurationsOutput listBucketIntelligentTieringConfigurations(ListBucketIntelligentTieringConfigurationsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListBucketIntelligentTieringConfigurations.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListBucketInventoryConfigurationsOutput listBucketInventoryConfigurations(ListBucketInventoryConfigurationsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListBucketInventoryConfigurations.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListBucketMetricsConfigurationsOutput listBucketMetricsConfigurations(ListBucketMetricsConfigurationsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListBucketMetricsConfigurations.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListBucketsOutput listBuckets(ListBucketsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListBuckets.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListDirectoryBucketsOutput listDirectoryBuckets(ListDirectoryBucketsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListDirectoryBuckets.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListMultipartUploadsOutput listMultipartUploads(ListMultipartUploadsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListMultipartUploads.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListObjectAnnotationsOutput listObjectAnnotations(ListObjectAnnotationsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListObjectAnnotations.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListObjectsOutput listObjects(ListObjectsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListObjects.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListObjectsV2Output listObjectsV2(ListObjectsV2Input input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListObjectsV2.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListObjectVersionsOutput listObjectVersions(ListObjectVersionsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListObjectVersions.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public ListPartsOutput listParts(ListPartsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, ListParts.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketAbacOutput putBucketAbac(PutBucketAbacInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketAbac.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketAccelerateConfigurationOutput putBucketAccelerateConfiguration(PutBucketAccelerateConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketAccelerateConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketAclOutput putBucketAcl(PutBucketAclInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketAcl.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketAnalyticsConfigurationOutput putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketAnalyticsConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketCorsOutput putBucketCors(PutBucketCorsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketCors.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketEncryptionOutput putBucketEncryption(PutBucketEncryptionInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketEncryption.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketIntelligentTieringConfigurationOutput putBucketIntelligentTieringConfiguration(PutBucketIntelligentTieringConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketIntelligentTieringConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketInventoryConfigurationOutput putBucketInventoryConfiguration(PutBucketInventoryConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketInventoryConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketLifecycleConfigurationOutput putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketLifecycleConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketLoggingOutput putBucketLogging(PutBucketLoggingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketLogging.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketMetricsConfigurationOutput putBucketMetricsConfiguration(PutBucketMetricsConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketMetricsConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketNotificationConfigurationOutput putBucketNotificationConfiguration(PutBucketNotificationConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketNotificationConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketOwnershipControlsOutput putBucketOwnershipControls(PutBucketOwnershipControlsInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketOwnershipControls.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketPolicyOutput putBucketPolicy(PutBucketPolicyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketPolicy.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketReplicationOutput putBucketReplication(PutBucketReplicationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketReplication.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketRequestPaymentOutput putBucketRequestPayment(PutBucketRequestPaymentInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketRequestPayment.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketTaggingOutput putBucketTagging(PutBucketTaggingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketTagging.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketVersioningOutput putBucketVersioning(PutBucketVersioningInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketVersioning.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutBucketWebsiteOutput putBucketWebsite(PutBucketWebsiteInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutBucketWebsite.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutObjectOutput putObject(PutObjectInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutObject.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutObjectAclOutput putObjectAcl(PutObjectAclInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutObjectAcl.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutObjectAnnotationOutput putObjectAnnotation(PutObjectAnnotationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutObjectAnnotation.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutObjectLegalHoldOutput putObjectLegalHold(PutObjectLegalHoldInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutObjectLegalHold.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutObjectLockConfigurationOutput putObjectLockConfiguration(PutObjectLockConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutObjectLockConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutObjectRetentionOutput putObjectRetention(PutObjectRetentionInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutObjectRetention.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutObjectTaggingOutput putObjectTagging(PutObjectTaggingInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutObjectTagging.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public PutPublicAccessBlockOutput putPublicAccessBlock(PutPublicAccessBlockInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, PutPublicAccessBlock.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public RenameObjectOutput renameObject(RenameObjectInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, RenameObject.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public RestoreObjectOutput restoreObject(RestoreObjectInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, RestoreObject.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public SelectObjectContentOutput selectObjectContent(SelectObjectContentInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, SelectObjectContent.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateBucketMetadataAnnotationTableConfigurationOutput updateBucketMetadataAnnotationTableConfiguration(UpdateBucketMetadataAnnotationTableConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateBucketMetadataAnnotationTableConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateBucketMetadataInventoryTableConfigurationOutput updateBucketMetadataInventoryTableConfiguration(UpdateBucketMetadataInventoryTableConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateBucketMetadataInventoryTableConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateBucketMetadataJournalTableConfigurationOutput updateBucketMetadataJournalTableConfiguration(UpdateBucketMetadataJournalTableConfigurationInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateBucketMetadataJournalTableConfiguration.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UpdateObjectEncryptionOutput updateObjectEncryption(UpdateObjectEncryptionInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UpdateObjectEncryption.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UploadPartOutput uploadPart(UploadPartInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UploadPart.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public UploadPartCopyOutput uploadPartCopy(UploadPartCopyInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, UploadPartCopy.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public WriteGetObjectResponseOutput writeGetObjectResponse(WriteGetObjectResponseInput input, RequestOverrideConfig overrideConfig) {
        try {
            return call(input, WriteGetObjectResponse.instance(), overrideConfig);
        } catch (CompletionException e) {
            throw unwrapAndThrow(e);
        }
    }

    @Override
    public S3Waiter waiter() {
        return new S3Waiter(this);
    }

}
