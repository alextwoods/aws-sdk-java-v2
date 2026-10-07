/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.smithyjava.SmithyJavaSerde;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.AccessControlPolicy;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.Grantee;
import software.amazon.awssdk.services.s3.model.LifecycleExpiration;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.Owner;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.Type;

/**
 * Runs the same S3 calls through a client on smithy-java serde and a client on v2's own serde, against the same
 * canned responses, and requires the two to agree: the same request line, headers and body bytes after the whole
 * v2 pipeline (endpoint rules, checksums, signing) has run on them, and equal responses or equal exceptions.
 *
 * <p>Bodies are compared byte for byte, which is what makes this worth having: rest-xml has many ways to produce
 * a different but plausible request (escaping, element names, flattening, namespaces) and S3 rejects or, worse,
 * misreads some of them.
 */
class SmithyJavaSerdeEquivalenceTest {

    private static final Pattern SIGNATURE =
        Pattern.compile("(chunk-signature=|x-amz-trailer-signature:)[0-9a-f]{64}");

    private static final String BUCKET = "serde-bucket";
    private static final String AWKWARD_KEY = "nested/path/with spaces/ü+%25&=?#.txt";

    @AfterEach
    void clearProperty() {
        System.clearProperty(SmithyJavaSerde.ENABLED_PROPERTY);
    }

    static Stream<Arguments> requestCases() {
        List<Arguments> cases = new ArrayList<>();
        cases.add(call("getObject", c -> c.getObject(r -> r.bucket(BUCKET).key(AWKWARD_KEY).range("bytes=0-9")
                                                          .ifMatch("\"etag\"").versionId("v 1")
                                                          .ifModifiedSince(Instant.ofEpochSecond(1_700_000_000L))
                                                          .responseContentType("text/plain"),
                                                     ResponseTransformer.toBytes())));
        cases.add(call("putObject", c -> c.putObject(r -> r.bucket(BUCKET).key("k").contentType("text/plain")
                                                          .metadata(metadata())
                                                          .acl(ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
                                                          .tagging("a=b&c=d")
                                                          .storageClass(StorageClass.STANDARD_IA)
                                                          .expires(Instant.ofEpochMilli(1_700_000_000_123L))
                                                          .contentLength(5L),
                                                     RequestBody.fromString("hello"))));
        cases.add(call("putObjectNoContentType", c -> c.putObject(r -> r.bucket(BUCKET).key("k"),
                                                                  RequestBody.fromBytes(new byte[] {1, 2, 3}))));
        cases.add(call("headObject", c -> c.headObject(r -> r.bucket(BUCKET).key(AWKWARD_KEY).partNumber(3))));
        cases.add(call("deleteObject", c -> c.deleteObject(r -> r.bucket(BUCKET).key("k").versionId("v1")
                                                                .bypassGovernanceRetention(true))));
        cases.add(call("listObjectsV2", c -> c.listObjectsV2(r -> r.bucket(BUCKET).prefix("a b/").delimiter("/")
                                                                  .maxKeys(10).continuationToken("tok+/=")
                                                                  .fetchOwner(true).startAfter("x"))));
        cases.add(call("listBuckets", c -> c.listBuckets()));
        cases.add(call("putObjectTagging", c -> c.putObjectTagging(r -> r.bucket(BUCKET).key("k").tagging(
            Tagging.builder().tagSet(Tag.builder().key("env").value("prod & \"test\"").build(),
                                     Tag.builder().key("x").value("<y>").build()).build()))));
        cases.add(call("deleteObjects", c -> c.deleteObjects(r -> r.bucket(BUCKET).delete(Delete.builder().quiet(true)
            .objects(ObjectIdentifier.builder().key("line\rbreak\nkey").build(),
                     ObjectIdentifier.builder().key("it's \"quoted\" & <tagged>").versionId("v").build())
            .build()))));
        cases.add(call("completeMultipartUpload", c -> c.completeMultipartUpload(r -> r.bucket(BUCKET).key("k")
            .uploadId("upload id/1").multipartUpload(CompletedMultipartUpload.builder().parts(
                CompletedPart.builder().partNumber(1).eTag("\"etag-1\"").build(),
                CompletedPart.builder().partNumber(2).eTag("\"etag-2\"").checksumCRC32("AAAAAA==").build())
                                                                                .build()))));
        cases.add(call("createMultipartUpload", c -> c.createMultipartUpload(r -> r.bucket(BUCKET).key("k")
                                                                                  .metadata(metadata()))));
        cases.add(call("uploadPart", c -> c.uploadPart(r -> r.bucket(BUCKET).key("k").uploadId("u+1").partNumber(7),
                                                       RequestBody.fromString("part"))));
        cases.add(call("copyObject", c -> c.copyObject(r -> r.sourceBucket("src").sourceKey("a b/c")
                                                            .destinationBucket(BUCKET).destinationKey("dest key")
                                                            .metadataDirective("REPLACE").metadata(metadata())
                                                            .contentType("text/plain"))));
        cases.add(call("putBucketPolicy", c -> c.putBucketPolicy(r -> r.bucket(BUCKET)
            .policy("{\"Version\":\"2012-10-17\",\"Statement\":[]}"))));
        cases.add(call("createBucket", c -> c.createBucket(r -> r.bucket(BUCKET).createBucketConfiguration(
            b -> b.locationConstraint(BucketLocationConstraint.EU_WEST_1)))));
        cases.add(call("createBucketNoBody", c -> c.createBucket(r -> r.bucket(BUCKET))));
        cases.add(call("putBucketLifecycleConfiguration", c -> c.putBucketLifecycleConfiguration(r -> r.bucket(BUCKET)
            .lifecycleConfiguration(BucketLifecycleConfiguration.builder().rules(LifecycleRule.builder()
                .id("rule").status(ExpirationStatus.ENABLED).filter(LifecycleRuleFilter.builder().prefix("logs/").build())
                .expiration(LifecycleExpiration.builder().date(Instant.parse("2030-01-01T00:00:00Z")).build())
                .build()).build()))));
        cases.add(call("putObjectAclKnownGap", c -> c.putObjectAcl(r -> r.bucket(BUCKET).key("k").accessControlPolicy(
            AccessControlPolicy.builder()
                               .owner(Owner.builder().id("owner-id").displayName("me").build())
                               .grants(Grant.builder().permission(Permission.READ)
                                            .grantee(Grantee.builder().type(Type.CANONICAL_USER).id("grantee-id").build())
                                            .build(),
                                       Grant.builder().permission(Permission.WRITE)
                                            .grantee(Grantee.builder().type(Type.GROUP)
                                                            .uri("http://acs.amazonaws.com/groups/global/AllUsers")
                                                            .build())
                                            .build())
                               .build()))));
        cases.add(call("getBucketLocation", c -> c.getBucketLocation(r -> r.bucket(BUCKET))));
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestCases")
    void requestsMatchV2(String name, Function<S3Client, Object> call) {
        if (name.endsWith("KnownGap")) {
            return;
        }
        Captured v2 = capture(false, call, CannedResponse.ok(""));
        Captured smithy = capture(true, call, CannedResponse.ok(""));

        if (System.getProperty("dumpBodies") != null) {
            System.out.println("DUMP " + name + " v2     [" + new String(v2.body, StandardCharsets.UTF_8) + "]");
            System.out.println("DUMP " + name + " smithy [" + new String(smithy.body, StandardCharsets.UTF_8) + "]");
        }
        assertThat(smithy.request.method()).isEqualTo(v2.request.method());
        assertThat(smithy.request.getUri()).isEqualTo(v2.request.getUri());
        assertThat(comparableHeaders(smithy.request)).isEqualTo(comparableHeaders(v2.request));
        assertThat(headerNameCase(smithy.request)).isEqualTo(headerNameCase(v2.request));
        assertThat(normalizeSignatures(new String(smithy.body, StandardCharsets.UTF_8)))
            .isEqualTo(normalizeSignatures(new String(v2.body, StandardCharsets.UTF_8)));
    }

    /**
     * Pins smithy-java 1.6.1's handling of {@code @xmlNamespace} with a prefix: the prefixed attribute is
     * written but its {@code xmlns:} declaration is not, so the document uses an undeclared prefix. v2 models
     * the declaration as a plain attribute and writes both. Nothing in this layer can add it -- the serializer
     * owns the element's attributes -- so this test records the difference rather than asserting it away. When
     * smithy-java emits the declaration, this test fails and the case moves back to {@link #requestsMatchV2}.
     */
    @Test
    void putObjectAclOmitsNamespaceDeclaration() {
        Function<S3Client, Object> call = findRequestCase("putObjectAclKnownGap");
        String v2 = new String(capture(false, call, CannedResponse.ok("")).body, StandardCharsets.UTF_8);
        String smithy = new String(capture(true, call, CannedResponse.ok("")).body, StandardCharsets.UTF_8);
        assertThat(v2).contains("<Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\">");
        assertThat(smithy).contains("<Grantee xsi:type=\"CanonicalUser\">");
        assertThat(smithy.replace("<Grantee xsi:type=", "<Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type="))
            .isEqualTo(v2);
    }

    /**
     * Pins smithy-java 1.6.1's reading of a flattened list whose item structure itself contains a flattened
     * list: the item's members after the inner list are dropped, and so is every item after the first.
     * {@code ListObjectsV2} hits it through {@code Contents} (flattened) containing
     * {@code ChecksumAlgorithm} (flattened), and loses {@code Size}, {@code StorageClass}, {@code Owner} and
     * all but the first object. This is silent data loss inside the codec, so it is pinned here and S3
     * responses with that shape cannot use smithy-java serde until it is fixed upstream.
     */
    @Test
    void listObjectsV2LosesMembersAfterNestedFlattenedList() {
        CannedResponse canned = findResponseCase("listObjectsV2KnownGap");
        Function<S3Client, Object> call = findResponseCall("listObjectsV2KnownGap");
        ListObjectsV2Response v2 = (ListObjectsV2Response) capture(false, call, canned).response;
        ListObjectsV2Response smithy = (ListObjectsV2Response) capture(true, call, canned).response;

        assertThat(v2.contents()).hasSize(2);
        assertThat(v2.contents().get(0).size()).isEqualTo(12345678901L);
        assertThat(v2.contents().get(0).owner().id()).isEqualTo("owner");

        assertThat(smithy.contents()).hasSize(1);
        assertThat(smithy.contents().get(0).size()).isNull();
        assertThat(smithy.contents().get(0).owner()).isNull();
        assertThat(smithy.contents().get(0).storageClass()).isNull();
        // Everything outside Contents still reads correctly, which is what makes the loss silent.
        assertThat(smithy.name()).isEqualTo(v2.name());
        assertThat(smithy.keyCount()).isEqualTo(v2.keyCount());
        assertThat(smithy.commonPrefixes()).isEqualTo(v2.commonPrefixes());
        assertThat(smithy.nextContinuationToken()).isEqualTo(v2.nextContinuationToken());
    }

    @SuppressWarnings("unchecked")
    private static Function<S3Client, Object> findRequestCase(String name) {
        return (Function<S3Client, Object>) requestCases()
            .filter(a -> name.equals(a.get()[0])).findFirst().orElseThrow().get()[1];
    }

    private static CannedResponse findResponseCase(String name) {
        return (CannedResponse) responseCases().filter(a -> name.equals(a.get()[0])).findFirst().orElseThrow().get()[1];
    }

    @SuppressWarnings("unchecked")
    private static Function<S3Client, Object> findResponseCall(String name) {
        return (Function<S3Client, Object>) responseCases()
            .filter(a -> name.equals(a.get()[0])).findFirst().orElseThrow().get()[2];
    }

    static Stream<Arguments> responseCases() {
        List<Arguments> cases = new ArrayList<>();
        cases.add(response("listObjectsV2KnownGap", CannedResponse.ok(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
            + "<Name>serde-bucket</Name><Prefix>a/</Prefix><KeyCount>2</KeyCount><MaxKeys>1000</MaxKeys>"
            + "<Delimiter>/</Delimiter><IsTruncated>true</IsTruncated><NextContinuationToken>next+/=</NextContinuationToken>"
            + "<Contents><Key>a/one &amp; two.txt</Key><LastModified>2023-11-14T22:13:20.000Z</LastModified>"
            + "<ETag>&quot;9b2cf535f27731c974343645a3985328&quot;</ETag><ChecksumAlgorithm>CRC32</ChecksumAlgorithm>"
            + "<Size>12345678901</Size><StorageClass>STANDARD</StorageClass>"
            + "<Owner><ID>owner</ID><DisplayName>me</DisplayName></Owner></Contents>"
            + "<Contents><Key>a/two</Key><LastModified>2023-11-14T22:13:20.5Z</LastModified><ETag>\"e\"</ETag>"
            + "<Size>0</Size><StorageClass>GLACIER_IR</StorageClass><RestoreStatus><IsRestoreInProgress>false"
            + "</IsRestoreInProgress></RestoreStatus></Contents>"
            + "<CommonPrefixes><Prefix>a/b/</Prefix></CommonPrefixes><CommonPrefixes><Prefix>a/c/</Prefix></CommonPrefixes>"
            + "<Unknown><Nested>ignored</Nested></Unknown>"
            + "</ListBucketResult>"),
                               c -> c.listObjectsV2(r -> r.bucket(BUCKET))));
        cases.add(response("listObjectsV2Empty", CannedResponse.ok(
            "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Name>b</Name><KeyCount>0</KeyCount>"
            + "<IsTruncated>false</IsTruncated></ListBucketResult>"),
                               c -> c.listObjectsV2(r -> r.bucket(BUCKET))));
        cases.add(response("listObjectsV2NoBody", CannedResponse.ok(""),
                               c -> c.listObjectsV2(r -> r.bucket(BUCKET))));
        cases.add(response("getObject", CannedResponse.ok("0123456789")
                                                      .header("Content-Type", "text/plain")
                                                      .header("ETag", "\"abc\"")
                                                      .header("Last-Modified", "Tue, 14 Nov 2023 22:13:20 GMT")
                                                      .header("Expires", "Wed, 15 Nov 2023 22:13:20 GMT")
                                                      .header("Content-Range", "bytes 0-9/100")
                                                      .header("Accept-Ranges", "bytes")
                                                      .header("x-amz-version-id", "v1")
                                                      .header("x-amz-meta-Color", "blue")
                                                      .header("x-amz-meta-lower", "x")
                                                      .header("x-amz-tagging-count", "3")
                                                      .header("x-amz-delete-marker", "false")
                                                      .header("x-amz-missing-meta", "2")
                                                      .header("x-amz-object-lock-retain-until-date", "2030-01-01T00:00:00.000Z")
                                                      .header("x-amz-restore",
                                                              "ongoing-request=\"false\", expiry-date=\"Fri, 21 Dec 2012 00:00:00 GMT\"")
                                                      .header("x-amz-checksum-crc32", "AAAAAA=="),
                               c -> {
                                   ResponseBytes<GetObjectResponse> bytes =
                                       c.getObject(r -> r.bucket(BUCKET).key("k"), ResponseTransformer.toBytes());
                                   return Arrays.asList(bytes.response(), bytes.asUtf8String());
                               }));
        cases.add(response("headObject", CannedResponse.ok("")
                                                       .header("Content-Length", "12345678901")
                                                       .header("Last-Modified", "Tue, 14 Nov 2023 22:13:20 GMT")
                                                       .header("ETag", "\"abc\"")
                                                       .header("x-amz-server-side-encryption", "aws:kms")
                                                       .header("x-amz-mp-parts-count", "4")
                                                       .header("x-amz-archive-status", "ARCHIVE_ACCESS")
                                                       .header("x-amz-meta-a", "1"),
                               c -> c.headObject(r -> r.bucket(BUCKET).key("k"))));
        cases.add(response("putObject", CannedResponse.ok("").header("ETag", "\"etag\"")
                                                      .header("x-amz-version-id", "v2")
                                                      .header("x-amz-checksum-crc32", "AAAAAA==")
                                                      .header("x-amz-server-side-encryption", "AES256"),
                               c -> c.putObject(r -> r.bucket(BUCKET).key("k"), RequestBody.fromString("x"))));
        cases.add(response("deleteObject", CannedResponse.status(204, "").header("x-amz-delete-marker", "true")
                                                         .header("x-amz-version-id", "v3"),
                               c -> c.deleteObject(r -> r.bucket(BUCKET).key("k"))));
        cases.add(response("completeMultipartUpload", CannedResponse.ok(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n"
            + "<CompleteMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
            + "<Location>http://serde-bucket.s3.amazonaws.com/k</Location><Bucket>serde-bucket</Bucket><Key>k</Key>"
            + "<ETag>&quot;3858f62230ac3c915f300c664312c11f-9&quot;</ETag></CompleteMultipartUploadResult>")
                                                                       .header("x-amz-version-id", "v4"),
                               c -> c.completeMultipartUpload(r -> r.bucket(BUCKET).key("k").uploadId("u"))));
        cases.add(response("completeMultipartUploadErrorIn200", CannedResponse.ok(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<Error><Code>InternalError</Code><Message>We encountered an internal error. Please try again.</Message>"
            + "<RequestId>656c76696e6727732072657175657374</RequestId>"
            + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId></Error>"),
                               c -> c.completeMultipartUpload(r -> r.bucket(BUCKET).key("k").uploadId("u"))));
        cases.add(response("copyObject", CannedResponse.ok(
            "<CopyObjectResult><LastModified>2009-10-12T17:50:30.000Z</LastModified>"
            + "<ETag>\"9b2cf535f27731c974343645a3985328\"</ETag><ChecksumCRC32>AAAAAA==</ChecksumCRC32></CopyObjectResult>")
                                                       .header("x-amz-copy-source-version-id", "sv"),
                               c -> c.copyObject(r -> r.sourceBucket("s").sourceKey("k").destinationBucket(BUCKET)
                                                       .destinationKey("d"))));
        cases.add(response("copyObjectErrorIn200", CannedResponse.ok(
            "<Error><Code>SlowDown</Code><Message>Reduce your request rate.</Message></Error>"),
                               c -> c.copyObject(r -> r.sourceBucket("s").sourceKey("k").destinationBucket(BUCKET)
                                                       .destinationKey("d"))));
        cases.add(response("deleteObjects", CannedResponse.ok(
            "<DeleteResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
            + "<Deleted><Key>a</Key><VersionId>v</VersionId><DeleteMarker>true</DeleteMarker></Deleted>"
            + "<Deleted><Key>b&#x0D;c</Key></Deleted>"
            + "<Error><Key>d</Key><Code>AccessDenied</Code><Message>Access Denied</Message></Error></DeleteResult>"),
                               c -> c.deleteObjects(r -> r.bucket(BUCKET).delete(d -> d.objects(o -> o.key("a"))))));
        cases.add(response("getBucketAcl", CannedResponse.ok(
            "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Owner><ID>o</ID>"
            + "<DisplayName>me</DisplayName></Owner><AccessControlList>"
            + "<Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\">"
            + "<ID>g</ID><DisplayName>them</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant>"
            + "<Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"Group\">"
            + "<URI>http://acs.amazonaws.com/groups/global/AllUsers</URI></Grantee><Permission>READ</Permission></Grant>"
            + "</AccessControlList></AccessControlPolicy>"),
                               c -> c.getBucketAcl(r -> r.bucket(BUCKET))));
        cases.add(response("getBucketLocation", CannedResponse.ok(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<LocationConstraint xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">eu-west-1</LocationConstraint>"),
                               c -> c.getBucketLocation(r -> r.bucket(BUCKET))));
        cases.add(response("getBucketLocationUsEast1", CannedResponse.ok(
            "<LocationConstraint xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"/>"),
                               c -> c.getBucketLocation(r -> r.bucket(BUCKET))));
        cases.add(response("getBucketPolicy", CannedResponse.ok("{\"Version\":\"2012-10-17\",\"Statement\":[]}"),
                               c -> c.getBucketPolicy(r -> r.bucket(BUCKET))));
        cases.add(response("createMultipartUpload", CannedResponse.ok(
            "<InitiateMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Bucket>b</Bucket>"
            + "<Key>k</Key><UploadId>VXBsb2FkIElEIGZvciA2aWWpbmcncyBteS1tb3ZpZS5tMnRzIHVwbG9hZA</UploadId>"
            + "</InitiateMultipartUploadResult>").header("x-amz-abort-date", "Wed, 28 Oct 2009 00:00:00 GMT")
                                                 .header("x-amz-abort-rule-id", "rule"),
                               c -> c.createMultipartUpload(r -> r.bucket(BUCKET).key("k"))));
        cases.add(response("listBuckets", CannedResponse.ok(
            "<ListAllMyBucketsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Owner><ID>o</ID></Owner>"
            + "<Buckets><Bucket><Name>a</Name><CreationDate>2019-12-11T23:32:47+00:00</CreationDate></Bucket>"
            + "<Bucket><Name>b</Name><CreationDate>2019-12-11T23:32:47.123Z</CreationDate>"
            + "<BucketRegion>us-west-2</BucketRegion></Bucket></Buckets></ListAllMyBucketsResult>"),
                               c -> c.listBuckets()));
        cases.add(response("getObjectTagging", CannedResponse.ok(
            "<Tagging xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><TagSet><Tag><Key>a</Key><Value>b &lt;c&gt;</Value>"
            + "</Tag><Tag><Key>e</Key><Value></Value></Tag></TagSet></Tagging>").header("x-amz-version-id", "v"),
                               c -> c.getObjectTagging(r -> r.bucket(BUCKET).key("k"))));
        cases.add(response("serviceError", CannedResponse.status(404,
            "<Error><Code>NoSuchKey</Code><Message>The resource you requested does not exist</Message>"
            + "<Resource>/mybucket/myfoto.jpg</Resource><RequestId>4442587FB7D0A2F9</RequestId></Error>"),
                               c -> c.headObject(r -> r.bucket(BUCKET).key("k"))));
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("responseCases")
    void responsesMatchV2(String name, CannedResponse canned, Function<S3Client, Object> call) {
        if (name.endsWith("KnownGap")) {
            return;
        }
        Object v2 = outcome(false, call, canned);
        Object smithy = outcome(true, call, canned);
        assertThat(smithy).isEqualTo(v2);
    }

    private static Object outcome(boolean smithy, Function<S3Client, Object> call, CannedResponse canned) {
        try {
            Object response = capture(smithy, call, canned).response;
            List<Object> outcome = new ArrayList<>();
            for (Object r : response instanceof List ? (List<?>) response : List.of(response)) {
                outcome.add(r);
                outcome.add(String.valueOf(r));
                if (r instanceof AwsResponse) {
                    AwsResponse aws = (AwsResponse) r;
                    outcome.add(aws.responseMetadata().requestId());
                    outcome.add(aws.sdkHttpResponse().statusCode());
                }
            }
            return outcome;
        } catch (AwsServiceException e) {
            return Arrays.asList(e.getClass().getName(), e.getMessage(), e.statusCode(), e.awsErrorDetails().errorCode(),
                                 e.awsErrorDetails().errorMessage(), e.requestId(), e.extendedRequestId(), e.retryable());
        } catch (RuntimeException e) {
            return Arrays.asList(e.getClass().getName(), e.getMessage());
        }
    }

    private static Arguments call(String name, Function<S3Client, Object> call) {
        return Arguments.of(name, call);
    }

    private static Arguments response(String name, CannedResponse canned, Function<S3Client, Object> call) {
        return Arguments.of(name, canned, call);
    }

    private static Map<String, String> metadata() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("Mixed-Case", "Value");
        metadata.put("lower", "v w");
        return metadata;
    }

    /**
     * Blanks out the per-chunk and trailer signatures of an {@code aws-chunked} body. Those are derived from the
     * request's own SigV4 signature, which covers {@code X-Amz-Date}, and the two arms are signed a moment
     * apart, so they can never match. Everything else in the body, the chunk sizes and the trailing checksum
     * included, still compares.
     */
    private static String normalizeSignatures(String body) {
        return SIGNATURE.matcher(body).replaceAll("$1<signature>");
    }

    private static Map<String, List<String>> comparableHeaders(SdkHttpRequest request) {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(request.headers());
        for (String h : Arrays.asList("Authorization", "X-Amz-Date", "amz-sdk-invocation-id")) {
            headers.remove(h);
        }
        return headers;
    }

    private static List<String> headerNameCase(SdkHttpRequest request) {
        return new ArrayList<>(request.headers().keySet());
    }

    private static Captured capture(boolean smithy, Function<S3Client, Object> call, CannedResponse canned) {
        System.setProperty(SmithyJavaSerde.ENABLED_PROPERTY, Boolean.toString(smithy));
        CapturingHttpClient http = new CapturingHttpClient(canned);
        try (S3Client client = S3Client.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(
                                           AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost:8000"))
                                       .forcePathStyle(true)
                                       .httpClient(http)
                                       .build()) {
            Object response = call.apply(client);
            return new Captured(http.request, http.body, response);
        }
    }

    static final class CannedResponse {
        private final int status;
        private final byte[] body;
        private final Map<String, String> headers = new LinkedHashMap<>();

        private CannedResponse(int status, String body) {
            this.status = status;
            this.body = body.getBytes(StandardCharsets.UTF_8);
            headers.put("x-amz-request-id", "REQID123");
            headers.put("x-amz-id-2", "EXTENDED/ID+2=");
        }

        static CannedResponse ok(String body) {
            return new CannedResponse(200, body);
        }

        static CannedResponse status(int status, String body) {
            return new CannedResponse(status, body);
        }

        CannedResponse header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        @Override
        public String toString() {
            return status + " " + headers;
        }
    }

    private static final class Captured {
        private final SdkHttpRequest request;
        private final byte[] body;
        private final Object response;

        private Captured(SdkHttpRequest request, byte[] body, Object response) {
            this.request = request;
            this.body = body;
            this.response = response;
        }
    }

    private static final class CapturingHttpClient implements SdkHttpClient {
        private final CannedResponse canned;
        private SdkHttpRequest request;
        private byte[] body = new byte[0];

        private CapturingHttpClient(CannedResponse canned) {
            this.canned = canned;
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest executeRequest) {
            this.request = executeRequest.httpRequest();
            executeRequest.contentStreamProvider().ifPresent(p -> {
                try {
                    this.body = p.newStream().readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(canned.status);
                    canned.headers.forEach(response::putHeader);
                    if (!canned.headers.containsKey("Content-Length")) {
                        response.putHeader("Content-Length", String.valueOf(canned.body.length));
                    }
                    return HttpExecuteResponse.builder()
                                              .response(response.build())
                                              .responseBody(AbortableInputStream.create(new ByteArrayInputStream(canned.body)))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        @Override
        public void close() {
        }
    }
}
