package com.example.demo;

import com.example.demo.dynamodb.client.DynamoDBClient;
import com.example.demo.interceptors.LoggingInterceptor;
import com.example.demo.interceptors.V2ExecutionInterceptor;
import com.example.demo.interceptors.V2InterceptorBridge;
import com.example.demo.transport.V2TransportBridge;
import com.example.demo.dynamodb.model.AttributeDefinition;
import com.example.demo.dynamodb.model.AttributeValue;
import com.example.demo.dynamodb.model.CreateTableInput;
import com.example.demo.dynamodb.model.GetItemInput;
import com.example.demo.dynamodb.model.GetItemOutput;
import com.example.demo.dynamodb.model.KeySchemaElement;
import com.example.demo.dynamodb.model.KeyType;
import com.example.demo.dynamodb.model.ListTablesInput;
import com.example.demo.dynamodb.model.ListTablesOutput;
import com.example.demo.dynamodb.model.ProvisionedThroughput;
import com.example.demo.dynamodb.model.PutItemInput;
import com.example.demo.dynamodb.model.ResourceNotFoundException;
import com.example.demo.dynamodb.model.ScalarAttributeType;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.auth.api.identity.IdentityResult;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;

/**
 * End-to-end demo proving the AWS SDK can run on the smithy-java runtime:
 * protocol (awsJson1_0), SigV4 signing, HTTP transport, and schema-based serde are all
 * smithy-java. The only thing generated from the AWS Smithy model (pulled from Maven
 * Central) is the typed client + shapes. No C2J anywhere.
 *
 * <p>Runs against DynamoDB Local (http://localhost:8000) so no real AWS account is needed.
 */
public final class Main {

    private static final String TABLE = "DemoSmithyJava";

    public static void main(String[] args) throws Exception {
        String endpoint = System.getProperty("ddb.endpoint", "http://localhost:8000");

        // DynamoDB Local accepts any non-empty credentials; SigV4 still runs for real.
        // Use AwsCredentialsResolver so identityType() reports the AwsCredentialsIdentity
        // interface that the SigV4 auth scheme looks up by.
        AwsCredentialsIdentity identity = AwsCredentialsIdentity.create("dummy", "dummy");
        AwsCredentialsResolver creds = new AwsCredentialsResolver() {
            @Override
            public IdentityResult<AwsCredentialsIdentity> resolveIdentity(Context properties) {
                return IdentityResult.of(identity);
            }
        };

        // A v2-style ExecutionInterceptor that logs the lifecycle and adds a custom HTTP
        // header. It's bridged onto smithy-java's pipeline by V2InterceptorBridge — proving
        // a customer's v2 interceptor keeps working with smithy-java underneath.
        V2ExecutionInterceptor v2Interceptor = new V2ExecutionInterceptor() {
            @Override
            public void beforeExecution(String operationName) {
                System.out.println("  [v2-bridge] beforeExecution  op=" + operationName);
            }

            @Override
            public V2HttpRequest modifyHttpRequest(V2HttpRequest httpRequest) {
                System.out.println("  [v2-bridge] modifyHttpRequest " + httpRequest.method()
                        + " -> adding header X-Demo-Interceptor");
                return httpRequest.addHeader("X-Demo-Interceptor", "v2-bridge");
            }

            @Override
            public void afterExecution(String operationName, Throwable error) {
                System.out.println("  [v2-bridge] afterExecution   op=" + operationName
                        + " error=" + (error == null ? "none" : error));
            }
        };

        // Wrap an AWS SDK v2 SdkHttpClient (URLConnection-based) as a smithy-java transport,
        // so every request below travels over the v2 HTTP stack while smithy-java drives the call.
        ClientTransport<HttpRequest, HttpResponse> v2Transport =
                new V2TransportBridge(UrlConnectionHttpClient.create());

        DynamoDBClient client = DynamoDBClient.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .addIdentityResolver(creds)
                .endpointResolver(EndpointResolver.staticEndpoint(endpoint))
                .addInterceptor(new LoggingInterceptor())            // native smithy-java interceptor
                .addInterceptor(new V2InterceptorBridge(v2Interceptor)) // v2 interceptor via the bridge
                .transport(v2Transport)                              // v2 SdkHttpClient via the transport bridge
                .build();

        System.out.println("== smithy-java DynamoDB demo (endpoint: " + endpoint + ") ==");

        createTableIfNeeded(client);
        putItem(client, "user-1", "Ada Lovelace");
        getItem(client, "user-1");
        listTables(client);
        getItemMissingTable(client);

        System.out.println("== done ==");
    }

    /**
     * The unhappy path: hit a table that doesn't exist. This proves the protocol layer
     * deserializes a service error into the correct <b>modeled, typed</b> exception
     * ({@link ResourceNotFoundException}) — the riskiest serde path, since every byte of the
     * error mapping is driven by the smithy-java runtime + the generated schema, not C2J.
     */
    private static void getItemMissingTable(DynamoDBClient client) {
        try {
            client.getItem(GetItemInput.builder()
                    .tableName("NoSuchTable_" + TABLE)
                    .key(Map.of("id", new AttributeValue.SMember("nobody")))
                    .build());
            System.out.println("[error]      UNEXPECTED: no exception thrown");
        } catch (ResourceNotFoundException e) {
            // Typed, modeled exception — exactly the v2 experience.
            System.out.println("[error]      caught typed " + e.getClass().getSimpleName()
                    + ": fault=" + e.getFault()
                    + " throttle=" + e.isThrottle()
                    + " msg=\"" + e.getMessage() + "\"");
        }
    }

    private static void createTableIfNeeded(DynamoDBClient client) {
        boolean exists = client.listTables(ListTablesInput.builder().build())
                .getTableNames().contains(TABLE);
        if (exists) {
            System.out.println("[createTable] table already exists, skipping");
            return;
        }
        client.createTable(CreateTableInput.builder()
                .tableName(TABLE)
                .attributeDefinitions(List.of(AttributeDefinition.builder()
                        .attributeName("id")
                        .attributeType(ScalarAttributeType.S)
                        .build()))
                .keySchema(List.of(KeySchemaElement.builder()
                        .attributeName("id")
                        .keyType(KeyType.HASH)
                        .build()))
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                .build());
        System.out.println("[createTable] created table " + TABLE);
    }

    private static void putItem(DynamoDBClient client, String id, String name) {
        client.putItem(PutItemInput.builder()
                .tableName(TABLE)
                .item(Map.of(
                        "id", new AttributeValue.SMember(id),
                        "name", new AttributeValue.SMember(name)))
                .build());
        System.out.println("[putItem]    wrote {id=" + id + ", name=" + name + "}");
    }

    private static void getItem(DynamoDBClient client, String id) {
        GetItemOutput out = client.getItem(GetItemInput.builder()
                .tableName(TABLE)
                .key(Map.of("id", new AttributeValue.SMember(id)))
                .build());
        Map<String, AttributeValue> item = out.getItem();
        String name = item.get("name") instanceof AttributeValue.SMember s ? s.s() : "<none>";
        System.out.println("[getItem]    read  {id=" + id + ", name=" + name + "}");
    }

    private static void listTables(DynamoDBClient client) {
        ListTablesOutput out = client.listTables(ListTablesInput.builder().build());
        System.out.println("[listTables] " + out.getTableNames());
    }
}
