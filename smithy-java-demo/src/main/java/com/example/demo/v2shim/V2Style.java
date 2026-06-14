package com.example.demo.v2shim;

import com.example.demo.dynamodb.client.DynamoDBClient;
import com.example.demo.dynamodb.model.AttributeValue;
import com.example.demo.dynamodb.model.GetItemInput;
import com.example.demo.dynamodb.model.PutItemInput;
import java.util.HashMap;
import java.util.Map;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity;
import software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.auth.api.identity.IdentityResult;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.endpoints.EndpointResolver;

/**
 * A deliberately tiny re-creation of the AWS SDK for Java <b>v2</b> public surface for
 * DynamoDB — {@code DynamoDbClient.builder()...build()}, {@code AttributeValue.fromS(..)},
 * {@code PutItemRequest}, {@code GetItemResponse} — whose method bodies do nothing but
 * translate to/from the smithy-java generated client and delegate the actual work to it.
 *
 * <p>This is the "Java SDK is a thin shell over smithy-java" thesis in miniature: the
 * customer-facing API names/shape are v2; every byte on the wire is produced by the
 * smithy-java runtime (protocol, SigV4, transport, serde). A real migration would
 * code-generate this façade for all 400+ services instead of hand-writing it.
 */
public final class V2Style {

    // ---- v2-shaped public API ------------------------------------------------

    /** Mirrors {@code software.amazon.awssdk.services.dynamodb.model.AttributeValue}. */
    public static final class AttributeValue {
        private final String s;

        private AttributeValue(String s) {
            this.s = s;
        }

        public static AttributeValue fromS(String s) {
            return new AttributeValue(s);
        }

        public String s() {
            return s;
        }
    }

    /** Mirrors v2 {@code PutItemRequest}. */
    public static final class PutItemRequest {
        private final String tableName;
        private final Map<String, AttributeValue> item;

        private PutItemRequest(Builder b) {
            this.tableName = b.tableName;
            this.item = b.item;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String tableName;
            private Map<String, AttributeValue> item = new HashMap<>();

            public Builder tableName(String tableName) {
                this.tableName = tableName;
                return this;
            }

            public Builder item(Map<String, AttributeValue> item) {
                this.item = item;
                return this;
            }

            public PutItemRequest build() {
                return new PutItemRequest(this);
            }
        }
    }

    /** Mirrors v2 {@code GetItemRequest}. */
    public static final class GetItemRequest {
        private final String tableName;
        private final Map<String, AttributeValue> key;

        private GetItemRequest(Builder b) {
            this.tableName = b.tableName;
            this.key = b.key;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String tableName;
            private Map<String, AttributeValue> key = new HashMap<>();

            public Builder tableName(String tableName) {
                this.tableName = tableName;
                return this;
            }

            public Builder key(Map<String, AttributeValue> key) {
                this.key = key;
                return this;
            }

            public GetItemRequest build() {
                return new GetItemRequest(this);
            }
        }
    }

    /** Mirrors v2 {@code GetItemResponse}. */
    public static final class GetItemResponse {
        private final Map<String, AttributeValue> item;

        private GetItemResponse(Map<String, AttributeValue> item) {
            this.item = item;
        }

        public Map<String, AttributeValue> item() {
            return item;
        }
    }

    /** Mirrors v2 {@code DynamoDbClient} (the subset we demo). */
    public static final class DynamoDbClient implements AutoCloseable {
        private final DynamoDBClient delegate; // the smithy-java generated client

        private DynamoDbClient(DynamoDBClient delegate) {
            this.delegate = delegate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public void putItem(PutItemRequest request) {
            delegate.putItem(PutItemInput.builder()
                    .tableName(request.tableName)
                    .item(toSmithy(request.item))
                    .build());
        }

        public GetItemResponse getItem(GetItemRequest request) {
            var out = delegate.getItem(GetItemInput.builder()
                    .tableName(request.tableName)
                    .key(toSmithy(request.key))
                    .build());
            return new GetItemResponse(fromSmithy(out.getItem()));
        }

        @Override
        public void close() {
            // delegate transport cleanup would go here in a real shim
        }

        /** Mirrors v2 {@code DynamoDbClientBuilder} (only what the demo needs). */
        public static final class Builder {
            private String region = "us-east-1";
            private String endpoint;
            private AwsCredentialsIdentity credentials =
                    AwsCredentialsIdentity.create("dummy", "dummy");

            public Builder region(String region) {
                this.region = region;
                return this;
            }

            public Builder endpointOverride(String endpoint) {
                this.endpoint = endpoint;
                return this;
            }

            public Builder credentials(String accessKeyId, String secretAccessKey) {
                this.credentials = AwsCredentialsIdentity.create(accessKeyId, secretAccessKey);
                return this;
            }

            public DynamoDbClient build() {
                AwsCredentialsResolver resolver = new AwsCredentialsResolver() {
                    @Override
                    public IdentityResult<AwsCredentialsIdentity> resolveIdentity(Context properties) {
                        return IdentityResult.of(credentials);
                    }
                };
                var b = DynamoDBClient.builder()
                        .putConfig(RegionSetting.REGION, region)
                        .addIdentityResolver(resolver);
                if (endpoint != null) {
                    b.endpointResolver(EndpointResolver.staticEndpoint(endpoint));
                }
                return new DynamoDbClient(b.build());
            }
        }
    }

    // ---- type mapping at the v2 <-> smithy-java boundary ----------------------

    private static Map<String, com.example.demo.dynamodb.model.AttributeValue> toSmithy(
            Map<String, AttributeValue> v2) {
        Map<String, com.example.demo.dynamodb.model.AttributeValue> out = new HashMap<>();
        v2.forEach((k, val) ->
                out.put(k, new com.example.demo.dynamodb.model.AttributeValue.SMember(val.s())));
        return out;
    }

    private static Map<String, AttributeValue> fromSmithy(
            Map<String, com.example.demo.dynamodb.model.AttributeValue> smithy) {
        Map<String, AttributeValue> out = new HashMap<>();
        smithy.forEach((k, val) -> {
            if (val instanceof com.example.demo.dynamodb.model.AttributeValue.SMember s) {
                out.put(k, AttributeValue.fromS(s.s()));
            }
        });
        return out;
    }

    private V2Style() {}

    // ---- demo entry point -----------------------------------------------------

    public static void main(String[] args) {
        String endpoint = System.getProperty("ddb.endpoint", "http://localhost:8000");

        // Note: this reads almost exactly like real AWS SDK v2 code.
        try (DynamoDbClient ddb = DynamoDbClient.builder()
                .region("us-east-1")
                .endpointOverride(endpoint)
                .credentials("dummy", "dummy")
                .build()) {

            System.out.println("== v2-style facade over smithy-java (endpoint: " + endpoint + ") ==");

            ddb.putItem(PutItemRequest.builder()
                    .tableName("DemoSmithyJava")
                    .item(Map.of(
                            "id", AttributeValue.fromS("user-2"),
                            "name", AttributeValue.fromS("Grace Hopper")))
                    .build());
            System.out.println("[v2.putItem] wrote {id=user-2, name=Grace Hopper}");

            GetItemResponse resp = ddb.getItem(GetItemRequest.builder()
                    .tableName("DemoSmithyJava")
                    .key(Map.of("id", AttributeValue.fromS("user-2")))
                    .build());
            System.out.println("[v2.getItem] read  {id=user-2, name="
                    + resp.item().get("name").s() + "}");

            System.out.println("== done — v2 API on top, smithy-java on the wire ==");
        }
    }
}
