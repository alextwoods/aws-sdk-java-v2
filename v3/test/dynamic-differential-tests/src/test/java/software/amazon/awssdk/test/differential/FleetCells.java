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

package software.amazon.awssdk.test.differential;

import static software.amazon.awssdk.test.differential.Values.cbor;
import static software.amazon.awssdk.test.differential.Values.map;
import static software.amazon.awssdk.test.differential.Values.utf8;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.test.differential.Matrix.CallShape;
import software.amazon.awssdk.test.differential.Matrix.Protocol;
import software.amazon.awssdk.test.differential.Matrix.Reply;
import software.amazon.awssdk.test.differential.Matrix.Scenario;
import software.amazon.awssdk.test.differential.Matrix.ScenarioBuilder;
import software.amazon.awssdk.test.differential.Matrix.Target;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The fleet scenarios: real generated service artifacts (services/*) against the Dynamic Client loaded from the
 * canonical model file vendored in the same service module, which is the file that artifact was generated from.
 *
 * <p>Selection (deterministic: the services, operations and replies are fixed in this class, not sampled): for each
 * launch protocol, one representative, high-traffic fleet service whose selected protocol (by launch precedence) is
 * that protocol and that models both a waiter and a paginated operation, so one service covers the finite, error,
 * paginated and waiter shapes; where that service also has the protocol's event stream or streaming blob operations
 * they are used too. Polly is added for the restJson1 streaming blob output, which Lambda lacks. The services are:
 * <ul>
 *   <li>awsJson1_0: DynamoDB (GetItem, ListTables, TableExists);</li>
 *   <li>awsJson1_1: Kinesis (DescribeStreamSummary, ListStreams, StreamExists, the SubscribeToShard output event stream
 *       routed by its consumer ARN);</li>
 *   <li>rpcv2Cbor: CloudWatch (selected over its awsJson1_0 and awsQuery declarations by launch precedence; with
 *       {@code awsQueryCompatible} error codes; GetDashboard, ListDashboards, AlarmExists);</li>
 *   <li>awsQuery: IAM (GetUser, ListUsers, UserExists);</li>
 *   <li>ec2Query: EC2 (DescribeAvailabilityZones, DescribeInstances, InstanceExists; EC2 models no errors, so its
 *       error cell is unmodeled);</li>
 *   <li>restJson1: Lambda (Invoke with a blob payload, GetFunction, ListFunctions, FunctionExists, the
 *       InvokeWithResponseStream output event stream) and Polly (SynthesizeSpeech streaming output);</li>
 *   <li>restXml: S3 (GetBucketTagging, AbortMultipartUpload, PutObject with flexible checksums, GetObject,
 *       ListObjectsV2, BucketExists over HEAD, the SelectObjectContent output event stream).</li>
 * </ul>
 * The input event stream cells are synthetic only: the fleet's input event streams are restJson1 bidirectional
 * streams (Transcribe Streaming and similar), whose HTTP/2 duplex exchange this scripted transport does not model; no
 * representative service of another protocol has one. The restJson1 streaming blob input is synthetic only for the
 * same selection reason (Lambda and Polly have none).
 *
 * <p>Shared defects (both clients behave identically, so they are outside this differential's oracle, but they limit
 * which real replies give meaningful cells): S3 keeps the {@code {Bucket}} label in the path of virtual-hosted
 * requests (the known defect of {@code S3GeneratedApiTest}), and a body-less S3 {@code HEAD} 404 has no error code, so
 * {@code BucketExists}'s {@code NotFound} retry acceptor cannot match; the S3 waiter scenario therefore polls once.
 */
final class FleetCells {
    private static final String EVENT_STREAM = "application/vnd.amazon.eventstream";

    private FleetCells() {
    }

    private static Target target(String module, Protocol protocol, String serviceId, String pkg, String prefix,
                                 String signingName) {
        return new Target("fleet:" + module, protocol, ShapeId.from(serviceId),
                          "software.amazon.awssdk.services." + pkg, prefix, signingName,
                          SyntheticCells.memoize(() -> DynamicModel.load(modelOf(module))));
    }

    /** The canonical model vendored in the service module, from which its artifact was generated. */
    private static Path modelOf(String module) {
        return Path.of(System.getProperty("v3.differential.services", "../../services"), module, "src", "main",
                       "smithy", "model.json");
    }

    static final Target DYNAMODB = target("dynamodb", Protocol.AWS_JSON_1_0, "com.amazonaws.dynamodb#DynamoDB_20120810",
                                          "dynamodb", "DynamoDb", "dynamodb");
    static final Target KINESIS = target("kinesis", Protocol.AWS_JSON_1_1, "com.amazonaws.kinesis#Kinesis_20131202",
                                         "kinesis", "Kinesis", "kinesis");
    static final Target CLOUDWATCH = target("cloudwatch", Protocol.RPCV2_CBOR,
                                            "com.amazonaws.cloudwatch#GraniteServiceVersion20100801", "cloudwatch",
                                            "CloudWatch", "monitoring");
    static final Target IAM = target("iam", Protocol.AWS_QUERY, "com.amazonaws.iam#AWSIdentityManagementV20100508",
                                     "iam", "Iam", "iam");
    static final Target EC2 = target("ec2", Protocol.EC2_QUERY, "com.amazonaws.ec2#AmazonEC2", "ec2", "Ec2", "ec2");
    static final Target LAMBDA = target("lambda", Protocol.REST_JSON_1, "com.amazonaws.lambda#AWSGirApiService",
                                        "lambda", "Lambda", "lambda");
    static final Target POLLY = target("polly", Protocol.REST_JSON_1, "com.amazonaws.polly#Parrot_v1", "polly",
                                       "Polly", "polly");
    static final Target S3 = target("s3", Protocol.REST_XML, "com.amazonaws.s3#AmazonS3", "s3", "S3", "s3");

    static List<Scenario> scenarios() {
        List<Scenario> out = new ArrayList<>();
        dynamoDb(out);
        kinesis(out);
        cloudWatch(out);
        iam(out);
        ec2(out);
        lambda(out);
        polly(out);
        s3(out);
        return out;
    }

    // -------------------------------------------------------------------------------------------------------------
    // awsJson1_0: DynamoDB

    private static final String DYNAMODB_NOT_FOUND = "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException";

    private static Reply dynamoDbJson(int status, String body) {
        return Reply.of(status, body, "Content-Type", "application/x-amz-json-1.0");
    }

    private static void dynamoDb(List<Scenario> out) {
        Map<String, Object> getItem = map("TableName", "Music",
                                          "Key", map("Artist", map("S", "No One You Know"), "Year", map("N", "2024")),
                                          "ConsistentRead", true, "ProjectionExpression", "#a, Plays",
                                          "ExpressionAttributeNames", map("#a", "Artist"));
        out.add(new ScenarioBuilder(DYNAMODB, CallShape.FINITE, "GetItem").input(getItem)
                    .reply(dynamoDbJson(200, "{\"Item\":{\"Artist\":{\"S\":\"No One You Know\"},"
                                             + "\"Plays\":{\"N\":\"7\"},"
                                             + "\"Tags\":{\"SS\":[\"a\",\"b\"]},\"Cover\":{\"B\":\"AAEC\"},"
                                             + "\"Live\":{\"BOOL\":true},\"Extra\":{\"NULL\":true},"
                                             + "\"Tracks\":{\"L\":[{\"S\":\"one\"},{\"M\":{\"n\":{\"N\":\"2\"}}}]}}}"))
                    .build());
        out.add(new ScenarioBuilder(DYNAMODB, CallShape.MODELED_ERROR, "GetItem").input(getItem)
                    .reply(dynamoDbJson(400, "{\"__type\":\"" + DYNAMODB_NOT_FOUND + "\","
                                             + "\"message\":\"Requested resource not found\"}"))
                    .build());
        Reply internal = dynamoDbJson(500, "{\"__type\":\"com.amazon.coral.service#InternalFailure\","
                                           + "\"message\":\"internal failure\"}");
        out.add(new ScenarioBuilder(DYNAMODB, CallShape.UNMODELED_ERROR, "GetItem").title("retried 500")
                    .input(getItem).reply(internal, internal).requests(2).build());
        out.add(new ScenarioBuilder(DYNAMODB, CallShape.PAGINATED, "ListTables").input(map("Limit", 2))
                    .reply(dynamoDbJson(200, "{\"TableNames\":[\"Albums\",\"Music\"],"
                                             + "\"LastEvaluatedTableName\":\"Music\"}"),
                           dynamoDbJson(200, "{\"TableNames\":[\"Songs\"]}"))
                    .requests(2).build());
        out.add(new ScenarioBuilder(DYNAMODB, CallShape.WAITER, "DescribeTable").waiter("TableExists")
                    .input(map("TableName", "Music"))
                    .reply(dynamoDbJson(400, "{\"__type\":\"" + DYNAMODB_NOT_FOUND + "\","
                                             + "\"message\":\"not yet\"}"),
                           dynamoDbJson(200, "{\"Table\":{\"TableName\":\"Music\",\"TableStatus\":\"CREATING\"}}"),
                           dynamoDbJson(200, "{\"Table\":{\"TableName\":\"Music\",\"TableStatus\":\"ACTIVE\","
                                             + "\"ItemCount\":3,\"CreationDateTime\":1.7E9}}"))
                    .requests(3).build());
    }

    // -------------------------------------------------------------------------------------------------------------
    // awsJson1_1: Kinesis

    private static Reply kinesisJson(int status, String body) {
        return Reply.of(status, body, "Content-Type", "application/x-amz-json-1.1");
    }

    private static String streamDescription(String status) {
        return "{\"StreamDescription\":{\"StreamName\":\"orders\",\"StreamARN\":\"arn:aws:kinesis:us-west-2:"
               + "123456789012:stream/orders\",\"StreamStatus\":\"" + status + "\",\"Shards\":[],"
               + "\"HasMoreShards\":false,\"RetentionPeriodHours\":24,\"StreamCreationTimestamp\":1.7E9,"
               + "\"EnhancedMonitoring\":[{\"ShardLevelMetrics\":[]}]}}";
    }

    private static void kinesis(List<Scenario> out) {
        Map<String, Object> summary = map("StreamName", "orders");
        out.add(new ScenarioBuilder(KINESIS, CallShape.FINITE, "DescribeStreamSummary").input(summary)
                    .reply(kinesisJson(200, "{\"StreamDescriptionSummary\":{\"StreamName\":\"orders\",\"StreamARN\":"
                                            + "\"arn:aws:kinesis:us-west-2:123456789012:stream/orders\","
                                            + "\"StreamStatus\":\"ACTIVE\",\"RetentionPeriodHours\":24,"
                                            + "\"StreamCreationTimestamp\":1.7E9,\"EnhancedMonitoring\":[],"
                                            + "\"OpenShardCount\":2,\"StreamModeDetails\":{\"StreamMode\":"
                                            + "\"ON_DEMAND\"}}}"))
                    .build());
        out.add(new ScenarioBuilder(KINESIS, CallShape.MODELED_ERROR, "DescribeStreamSummary").input(summary)
                    .reply(kinesisJson(400, "{\"__type\":\"ResourceNotFoundException\","
                                            + "\"message\":\"Stream orders not found\"}"))
                    .build());
        out.add(new ScenarioBuilder(KINESIS, CallShape.PAGINATED, "ListStreams").input(map("Limit", 2))
                    .reply(kinesisJson(200, "{\"StreamNames\":[\"a\",\"b\"],\"HasMoreStreams\":true,"
                                            + "\"NextToken\":\"token-2\"}"),
                           kinesisJson(200, "{\"StreamNames\":[\"c\"],\"HasMoreStreams\":false}"))
                    .requests(2).build());
        out.add(new ScenarioBuilder(KINESIS, CallShape.WAITER, "DescribeStream").waiter("StreamExists")
                    .input(map("StreamName", "orders"))
                    .reply(kinesisJson(200, streamDescription("CREATING")),
                           kinesisJson(200, streamDescription("ACTIVE")))
                    .requests(2).build());
        List<byte[]> frames = List.of(
            SyntheticCells.frame("event", ":event-type", "initial-response", "application/json",
                                 new LinkedHashMap<>(), utf8("{}")),
            SyntheticCells.frame("event", ":event-type", "SubscribeToShardEvent", "application/json",
                                 new LinkedHashMap<>(),
                                 utf8("{\"Records\":[{\"SequenceNumber\":\"4959\",\"ApproximateArrivalTimestamp\":"
                                      + "1.7E9,\"Data\":\"aGVsbG8=\",\"PartitionKey\":\"p-1\"}],"
                                      + "\"ContinuationSequenceNumber\":\"4959\",\"MillisBehindLatest\":0}")),
            SyntheticCells.frame("event", ":event-type", "SubscribeToShardEvent", "application/json",
                                 new LinkedHashMap<>(),
                                 utf8("{\"Records\":[],\"ContinuationSequenceNumber\":\"4960\","
                                      + "\"MillisBehindLatest\":5}")));
        out.add(new ScenarioBuilder(KINESIS, CallShape.EVENT_STREAM_OUTPUT, "SubscribeToShard")
                    .input(map("ConsumerARN", "arn:aws:kinesis:us-west-2:123456789012:stream/orders/consumer/"
                                              + "reader:1700000000",
                               "ShardId", "shardId-000000000000", "StartingPosition", map("Type", "LATEST")))
                    .reply(Reply.frames(200, frames, "Content-Type", EVENT_STREAM)).events(2).build());
    }

    // -------------------------------------------------------------------------------------------------------------
    // rpcv2Cbor: CloudWatch

    private static Reply cloudWatchCbor(int status, Map<String, Object> body, String... headers) {
        List<String> pairs = new ArrayList<>(List.of("Content-Type", "application/cbor", "smithy-protocol",
                                                     "rpc-v2-cbor"));
        pairs.addAll(List.of(headers));
        return Reply.of(status, cbor(body), pairs.toArray(String[]::new));
    }

    private static void cloudWatch(List<Scenario> out) {
        Map<String, Object> dashboard = map("DashboardName", "ops");
        out.add(new ScenarioBuilder(CLOUDWATCH, CallShape.FINITE, "GetDashboard").input(dashboard)
                    .reply(cloudWatchCbor(200, map("DashboardArn", "arn:aws:cloudwatch::123456789012:dashboard/ops",
                                                   "DashboardBody", "{\"widgets\":[]}", "DashboardName", "ops")))
                    .build());
        out.add(new ScenarioBuilder(CLOUDWATCH, CallShape.MODELED_ERROR, "GetDashboard").input(dashboard)
                    .reply(cloudWatchCbor(404, map("__type", "com.amazonaws.cloudwatch#DashboardNotFoundError",
                                                   "message", "Dashboard ops does not exist"),
                                          "x-amzn-query-error", "ResourceNotFound;Sender"))
                    .build());
        Reply internal = cloudWatchCbor(500, map("__type", "com.amazonaws.cloudwatch#InternalFailure",
                                                 "message", "internal failure"));
        out.add(new ScenarioBuilder(CLOUDWATCH, CallShape.UNMODELED_ERROR, "GetDashboard").title("retried 500")
                    .input(dashboard).reply(internal, internal).requests(2).build());
        out.add(new ScenarioBuilder(CLOUDWATCH, CallShape.PAGINATED, "ListDashboards")
                    .input(map("DashboardNamePrefix", "o"))
                    .reply(cloudWatchCbor(200, map("DashboardEntries", List.of(map("DashboardName", "ops", "Size", 10),
                                                                               map("DashboardName", "ops2")),
                                                   "NextToken", "n-2")),
                           cloudWatchCbor(200, map("DashboardEntries", List.of(map("DashboardName", "ops3")))))
                    .requests(2).build());
        out.add(new ScenarioBuilder(CLOUDWATCH, CallShape.WAITER, "DescribeAlarms").waiter("AlarmExists")
                    .input(map("AlarmNames", List.of("cpu-high")))
                    .reply(cloudWatchCbor(200, map("MetricAlarms", List.of())),
                           cloudWatchCbor(200, map("MetricAlarms", List.of(map("AlarmName", "cpu-high",
                                                                               "StateValue", "OK")))))
                    .requests(2).build());
    }

    // -------------------------------------------------------------------------------------------------------------
    // awsQuery: IAM

    private static final String IAM_NS = "https://iam.amazonaws.com/doc/2010-05-08/";

    private static Reply iamXml(String operation, String result) {
        return Reply.of(200, "<" + operation + "Response xmlns=\"" + IAM_NS + "\"><" + operation + "Result>" + result
                             + "</" + operation + "Result><ResponseMetadata><RequestId>iam-request</RequestId>"
                             + "</ResponseMetadata></" + operation + "Response>", "Content-Type", "text/xml");
    }

    private static Reply iamError(int status, String code, String message) {
        return Reply.of(status, "<ErrorResponse xmlns=\"" + IAM_NS + "\"><Error><Type>Sender</Type><Code>" + code
                                + "</Code><Message>" + message + "</Message></Error><RequestId>iam-error</RequestId>"
                                + "</ErrorResponse>", "Content-Type", "text/xml");
    }

    private static String iamUser(String name) {
        return "<Path>/</Path><UserName>" + name + "</UserName><UserId>AIDA" + name.toUpperCase() + "</UserId><Arn>"
               + "arn:aws:iam::123456789012:user/" + name + "</Arn><CreateDate>2024-01-02T03:04:05Z</CreateDate>";
    }

    private static void iam(List<Scenario> out) {
        Map<String, Object> getUser = map("UserName", "alice");
        Reply user = iamXml("GetUser", "<User>" + iamUser("alice") + "<Tags><member><Key>team</Key><Value>sdk"
                                       + "</Value></member></Tags></User>");
        out.add(new ScenarioBuilder(IAM, CallShape.FINITE, "GetUser").input(getUser).reply(user).build());
        Reply noSuchEntity = iamError(404, "NoSuchEntity", "The user with name alice cannot be found.");
        out.add(new ScenarioBuilder(IAM, CallShape.MODELED_ERROR, "GetUser").input(getUser).reply(noSuchEntity)
                    .build());
        Reply unavailable = iamError(503, "ServiceUnavailable", "try again");
        out.add(new ScenarioBuilder(IAM, CallShape.UNMODELED_ERROR, "GetUser").title("retried 503").input(getUser)
                    .reply(unavailable, unavailable).requests(2).build());
        out.add(new ScenarioBuilder(IAM, CallShape.PAGINATED, "ListUsers").input(map("PathPrefix", "/", "MaxItems", 2))
                    .reply(iamXml("ListUsers", "<Users><member>" + iamUser("alice") + "</member><member>"
                                               + iamUser("bob") + "</member></Users><IsTruncated>true</IsTruncated>"
                                               + "<Marker>m-2</Marker>"),
                           iamXml("ListUsers", "<Users><member>" + iamUser("carol") + "</member></Users>"
                                               + "<IsTruncated>false</IsTruncated>"))
                    .requests(2).build());
        out.add(new ScenarioBuilder(IAM, CallShape.WAITER, "GetUser").waiter("UserExists").input(getUser)
                    .reply(noSuchEntity, user).requests(2).build());
    }

    // -------------------------------------------------------------------------------------------------------------
    // ec2Query: EC2

    private static final String EC2_NS = "http://ec2.amazonaws.com/doc/2016-11-15/";

    private static Reply ec2Xml(String operation, String body) {
        return Reply.of(200, "<" + operation + "Response xmlns=\"" + EC2_NS + "\"><requestId>ec2-request</requestId>"
                             + body + "</" + operation + "Response>", "Content-Type", "text/xml;charset=UTF-8");
    }

    private static Reply ec2Error(int status, String code, String message) {
        return Reply.of(status, "<Response><Errors><Error><Code>" + code + "</Code><Message>" + message
                                + "</Message></Error></Errors><RequestID>ec2-error</RequestID></Response>",
                        "Content-Type", "text/xml;charset=UTF-8");
    }

    private static String reservation(String id, String instance) {
        return "<item><reservationId>" + id + "</reservationId><ownerId>123456789012</ownerId><instancesSet><item>"
               + "<instanceId>" + instance + "</instanceId><instanceType>t3.micro</instanceType><instanceState><code>16"
               + "</code><name>running</name></instanceState></item></instancesSet></item>";
    }

    private static void ec2(List<Scenario> out) {
        Map<String, Object> zones = map("ZoneNames", List.of("us-west-2a", "us-west-2b"), "AllAvailabilityZones", true,
                                        "Filters", List.of(map("Name", "state", "Values", List.of("available"))));
        out.add(new ScenarioBuilder(EC2, CallShape.FINITE, "DescribeAvailabilityZones").input(zones)
                    .reply(ec2Xml("DescribeAvailabilityZones",
                                  "<availabilityZoneInfo><item><zoneName>us-west-2a</zoneName><zoneState>available"
                                  + "</zoneState><regionName>us-west-2</regionName><zoneId>usw2-az1</zoneId>"
                                  + "<messageSet/></item></availabilityZoneInfo>"))
                    .build());
        out.add(new ScenarioBuilder(EC2, CallShape.UNMODELED_ERROR, "DescribeAvailabilityZones")
                    .title("non-retryable 400").input(zones)
                    .reply(ec2Error(400, "UnauthorizedOperation", "You are not authorized to perform this operation."))
                    .build());
        Map<String, Object> instances = map("InstanceIds", List.of("i-1"), "MaxResults", 5);
        out.add(new ScenarioBuilder(EC2, CallShape.PAGINATED, "DescribeInstances").input(instances)
                    .reply(ec2Xml("DescribeInstances", "<reservationSet>" + reservation("r-1", "i-1")
                                                       + reservation("r-2", "i-2") + "</reservationSet><nextToken>n-2"
                                                       + "</nextToken>"),
                           ec2Xml("DescribeInstances", "<reservationSet>" + reservation("r-3", "i-3")
                                                       + "</reservationSet>"))
                    .requests(2).build());
        out.add(new ScenarioBuilder(EC2, CallShape.WAITER, "DescribeInstances").waiter("InstanceExists")
                    .input(map("InstanceIds", List.of("i-1")))
                    .reply(ec2Error(400, "InvalidInstanceID.NotFound", "The instance ID 'i-1' does not exist"),
                           ec2Xml("DescribeInstances", "<reservationSet>" + reservation("r-1", "i-1")
                                                       + "</reservationSet>"))
                    .requests(2).build());
    }

    // -------------------------------------------------------------------------------------------------------------
    // restJson1: Lambda and Polly

    private static void lambda(List<Scenario> out) {
        out.add(new ScenarioBuilder(LAMBDA, CallShape.FINITE, "Invoke")
                    .input(map("FunctionName", "my-function", "InvocationType", "RequestResponse", "LogType", "Tail",
                               "Qualifier", "live", "Payload", utf8("{\"x\":1,\"y\":\"\u00e9\"}")))
                    .reply(Reply.of(200, "{\"ok\":true}", "Content-Type", "application/json",
                                    "X-Amz-Executed-Version", "3", "X-Amz-Log-Result", "bG9nIGxpbmU="))
                    .build());
        Map<String, Object> getFunction = map("FunctionName", "my-function", "Qualifier", "live");
        Reply notFound = Reply.of(404, "{\"Type\":\"User\",\"Message\":\"Function not found: my-function\"}",
                                  "Content-Type", "application/json", "X-Amzn-ErrorType",
                                  "ResourceNotFoundException:http://internal.amazon.com/coral/com.amazonaws.lambda/");
        out.add(new ScenarioBuilder(LAMBDA, CallShape.MODELED_ERROR, "GetFunction").input(getFunction).reply(notFound)
                    .build());
        out.add(new ScenarioBuilder(LAMBDA, CallShape.PAGINATED, "ListFunctions")
                    .input(map("FunctionVersion", "ALL", "MaxItems", 2))
                    .reply(Reply.of(200, "{\"Functions\":[{\"FunctionName\":\"a\",\"Runtime\":\"java21\"},"
                                         + "{\"FunctionName\":\"b\",\"MemorySize\":512}],\"NextMarker\":\"m-2\"}",
                                    "Content-Type", "application/json"),
                           Reply.of(200, "{\"Functions\":[{\"FunctionName\":\"c\"}]}", "Content-Type",
                                    "application/json"))
                    .requests(2).build());
        out.add(new ScenarioBuilder(LAMBDA, CallShape.WAITER, "GetFunction").waiter("FunctionExists")
                    .input(getFunction)
                    .reply(notFound, Reply.of(200, "{\"Configuration\":{\"FunctionName\":\"my-function\",\"State\":"
                                                   + "\"Active\",\"Version\":\"3\"},\"Tags\":{\"team\":\"sdk\"}}",
                                              "Content-Type", "application/json"))
                    .requests(2).build());
        Map<String, HeaderValue> none = new LinkedHashMap<>();
        List<byte[]> frames = List.of(
            SyntheticCells.frame("event", ":event-type", "PayloadChunk", "application/octet-stream", none,
                                 utf8("{\"part\":1,")),
            SyntheticCells.frame("event", ":event-type", "PayloadChunk", "application/octet-stream", none,
                                 utf8("\"done\":true}")),
            SyntheticCells.frame("event", ":event-type", "InvokeComplete", "application/json", none,
                                 utf8("{\"LogResult\":\"bG9n\"}")));
        out.add(new ScenarioBuilder(LAMBDA, CallShape.EVENT_STREAM_OUTPUT, "InvokeWithResponseStream")
                    .input(map("FunctionName", "my-function", "InvocationType", "RequestResponse",
                               "Payload", utf8("{\"stream\":true}")))
                    .reply(Reply.frames(200, frames, "Content-Type", EVENT_STREAM, "X-Amz-Executed-Version", "3"))
                    .events(3).build());
    }

    private static void polly(List<Scenario> out) {
        byte[] audio = new byte[] {'I', 'D', '3', 4, 0, 0, (byte) 0xff, (byte) 0xfb, 0x10, 0x00};
        out.add(new ScenarioBuilder(POLLY, CallShape.STREAMING_OUTPUT, "SynthesizeSpeech")
                    .input(map("OutputFormat", "mp3", "Text", "Hello <b>world</b>", "TextType", "text",
                               "VoiceId", "Joanna", "Engine", "neural", "SampleRate", "24000"))
                    .reply(Reply.of(200, audio, "Content-Type", "audio/mpeg", "x-amzn-RequestCharacters", "11"))
                    .build());
    }

    // -------------------------------------------------------------------------------------------------------------
    // restXml: S3

    private static final String S3_NS = "http://s3.amazonaws.com/doc/2006-03-01/";
    private static final String BUCKET = "amzn-s3-demo-bucket";

    private static Reply s3Xml(String body, String... headers) {
        List<String> pairs = new ArrayList<>(List.of("Content-Type", "application/xml"));
        pairs.addAll(List.of(headers));
        return Reply.of(200, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + body, pairs.toArray(String[]::new));
    }

    private static void s3(List<Scenario> out) {
        out.add(new ScenarioBuilder(S3, CallShape.FINITE, "GetBucketTagging")
                    .input(map("Bucket", BUCKET, "ExpectedBucketOwner", "123456789012"))
                    .reply(s3Xml("<Tagging xmlns=\"" + S3_NS + "\"><TagSet><Tag><Key>team</Key><Value>sdk</Value>"
                                 + "</Tag><Tag><Key>env</Key><Value>a&amp;b</Value></Tag></TagSet></Tagging>"))
                    .build());
        out.add(new ScenarioBuilder(S3, CallShape.MODELED_ERROR, "AbortMultipartUpload")
                    .input(map("Bucket", BUCKET, "Key", "dir/my object+1.txt", "UploadId", "upload-1"))
                    .reply(Reply.of(404, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error><Code>NoSuchUpload</Code>"
                                         + "<Message>The specified upload does not exist.</Message><UploadId>upload-1"
                                         + "</UploadId><RequestId>s3-request</RequestId><HostId>host-1</HostId>"
                                         + "</Error>", "Content-Type", "application/xml"))
                    .build());
        Reply internal = Reply.of(500, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error><Code>InternalError</Code>"
                                       + "<Message>We encountered an internal error.</Message><RequestId>s3-request"
                                       + "</RequestId></Error>", "Content-Type", "application/xml");
        out.add(new ScenarioBuilder(S3, CallShape.UNMODELED_ERROR, "GetBucketTagging").title("retried 500")
                    .input(map("Bucket", BUCKET)).reply(internal, internal).requests(2).build());
        byte[] object = utf8("line one\nline two \u00e9\n");
        out.add(new ScenarioBuilder(S3, CallShape.STREAMING_INPUT, "PutObject")
                    .input(map("Bucket", BUCKET, "Key", "dir/my object+1.txt", "ContentType", "text/plain",
                               "Metadata", map("origin", "differential"), "StorageClass", "STANDARD_IA"))
                    .requestBody(object)
                    .reply(Reply.of(200, new byte[0], "ETag", "\"etag-1\"", "x-amz-checksum-crc32", "AAAAAA==",
                                    "x-amz-version-id", "v-1"))
                    .build());
        out.add(new ScenarioBuilder(S3, CallShape.STREAMING_OUTPUT, "GetObject")
                    .input(map("Bucket", BUCKET, "Key", "dir/my object+1.txt", "Range", "bytes=0-7",
                               "ResponseContentType", "text/plain"))
                    .reply(Reply.of(200, utf8("line one"), "Content-Type", "text/plain", "Content-Length", "8",
                                    "ETag", "\"etag-1\"", "Last-Modified", "Wed, 01 May 2024 10:00:00 GMT",
                                    "x-amz-meta-origin", "differential"))
                    .build());
        out.add(new ScenarioBuilder(S3, CallShape.PAGINATED, "ListObjectsV2")
                    .input(map("Bucket", BUCKET, "Prefix", "dir/", "MaxKeys", 2))
                    .reply(s3Xml("<ListBucketResult xmlns=\"" + S3_NS + "\"><Name>" + BUCKET + "</Name><Prefix>dir/"
                                 + "</Prefix><KeyCount>2</KeyCount><MaxKeys>2</MaxKeys><IsTruncated>true</IsTruncated>"
                                 + "<Contents><Key>dir/a</Key><Size>1</Size><ETag>\"e-a\"</ETag></Contents><Contents>"
                                 + "<Key>dir/b</Key><Size>2</Size></Contents><NextContinuationToken>c-2"
                                 + "</NextContinuationToken></ListBucketResult>"),
                           s3Xml("<ListBucketResult xmlns=\"" + S3_NS + "\"><Name>" + BUCKET + "</Name><Prefix>dir/"
                                 + "</Prefix><KeyCount>1</KeyCount><MaxKeys>2</MaxKeys><IsTruncated>false"
                                 + "</IsTruncated><Contents><Key>dir/c</Key><Size>3</Size></Contents>"
                                 + "<ContinuationToken>c-2</ContinuationToken></ListBucketResult>"))
                    .requests(2).build());
        // One poll: BucketExists retries on NotFound, which neither client can match for a body-less HEAD 404 (the
        // error code is null in both; see the class comment), so the retry acceptor is exercised by the synthetic
        // restXml waiter instead.
        out.add(new ScenarioBuilder(S3, CallShape.WAITER, "HeadBucket").waiter("BucketExists")
                    .input(map("Bucket", BUCKET))
                    .reply(Reply.of(200, new byte[0], "x-amz-bucket-region", "us-west-2",
                                    "x-amz-bucket-location-type", "AvailabilityZone"))
                    .build());
        Map<String, HeaderValue> none = new LinkedHashMap<>();
        List<byte[]> frames = List.of(
            SyntheticCells.frame("event", ":event-type", "Records", "application/octet-stream", none,
                                 utf8("a,1\nb,2\n")),
            SyntheticCells.frame("event", ":event-type", "Stats", "text/xml", none,
                                 utf8("<Stats xmlns=\"\"><BytesScanned>16</BytesScanned><BytesProcessed>16"
                                      + "</BytesProcessed><BytesReturned>8</BytesReturned></Stats>")),
            SyntheticCells.frame("event", ":event-type", "End", "application/octet-stream", none, new byte[0]));
        out.add(new ScenarioBuilder(S3, CallShape.EVENT_STREAM_OUTPUT, "SelectObjectContent")
                    .input(map("Bucket", BUCKET, "Key", "data/rows.csv", "Expression", "SELECT * FROM S3Object s",
                               "ExpressionType", "SQL",
                               "InputSerialization", map("CSV", map("FileHeaderInfo", "NONE"), "CompressionType",
                                                         "NONE"),
                               "OutputSerialization", map("CSV", map("FieldDelimiter", ","))))
                    .reply(Reply.frames(200, frames, "Content-Type", EVENT_STREAM)).events(3).build());
    }
}
