package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.time.Instant;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.PresenceTracker;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * The established temporary security credentials of the session.
 *
 * <p><b>Directory buckets</b> - These session credentials are only supported for the authentication and authorization
 * of Zonal endpoint API operations on directory buckets.
 */
@SmithyGenerated
public final class SessionCredentials implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas1.SESSION_CREDENTIALS;
    private static final Schema $SCHEMA_ACCESS_KEY_ID = $SCHEMA.member("AccessKeyId");
    private static final Schema $SCHEMA_SECRET_ACCESS_KEY = $SCHEMA.member("SecretAccessKey");
    private static final Schema $SCHEMA_SESSION_TOKEN = $SCHEMA.member("SessionToken");
    private static final Schema $SCHEMA_EXPIRATION = $SCHEMA.member("Expiration");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String accessKeyId;
    private final transient String secretAccessKey;
    private final transient String sessionToken;
    private final transient Instant expiration;

    private SessionCredentials(Builder builder) {
        this.accessKeyId = builder.accessKeyId;
        this.secretAccessKey = builder.secretAccessKey;
        this.sessionToken = builder.sessionToken;
        this.expiration = builder.expiration;
    }

    /**
     * A unique identifier that's associated with a secret access key. The access key ID and the secret access key are
     * used together to sign programmatic Amazon Web Services requests cryptographically.
     */
    public String getAccessKeyId() {
        return accessKeyId;
    }

    /**
     * A key that's used with the access key ID to cryptographically sign programmatic Amazon Web Services requests.
     * Signing a request identifies the sender and prevents the request from being altered.
     */
    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    /**
     * A part of the temporary security credentials. The session token is used to validate the temporary security
     * credentials.
     *
     * <pre>{@code
     * </p>
     *
     * }</pre>
     */
    public String getSessionToken() {
        return sessionToken;
    }

    /**
     * Temporary security credentials expire after a specified interval. After temporary credentials expire, any calls
     * that you make with those credentials will fail. So you must generate a new set of temporary credentials.
     * Temporary credentials cannot be extended or refreshed beyond the original specified interval.
     */
    public Instant getExpiration() {
        return expiration;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        SessionCredentials that = (SessionCredentials) other;
        return Objects.equals(this.accessKeyId, that.accessKeyId)
               && Objects.equals(this.secretAccessKey, that.secretAccessKey)
               && Objects.equals(this.sessionToken, that.sessionToken)
               && Objects.equals(this.expiration, that.expiration);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(accessKeyId);
        $hc = 31 * $hc + Objects.hashCode(secretAccessKey);
        $hc = 31 * $hc + Objects.hashCode(sessionToken);
        $hc = 31 * $hc + Objects.hashCode(expiration);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_ACCESS_KEY_ID, accessKeyId);
        serializer.writeString($SCHEMA_SECRET_ACCESS_KEY, secretAccessKey);
        serializer.writeString($SCHEMA_SESSION_TOKEN, sessionToken);
        serializer.writeTimestamp($SCHEMA_EXPIRATION, expiration);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_ACCESS_KEY_ID, member, accessKeyId);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_SECRET_ACCESS_KEY, member, secretAccessKey);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_SESSION_TOKEN, member, sessionToken);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, expiration);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link SessionCredentials}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.accessKeyId(this.accessKeyId);
        builder.secretAccessKey(this.secretAccessKey);
        builder.sessionToken(this.sessionToken);
        builder.expiration(this.expiration);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link SessionCredentials}.
     */
    public static final class Builder implements ShapeBuilder<SessionCredentials> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        private Instant expiration;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * A unique identifier that's associated with a secret access key. The access key ID and the secret access key are
         * used together to sign programmatic Amazon Web Services requests cryptographically.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = Objects.requireNonNull(accessKeyId, "accessKeyId cannot be null");
            tracker.setMember($SCHEMA_ACCESS_KEY_ID);
            return this;
        }

        /**
         * A key that's used with the access key ID to cryptographically sign programmatic Amazon Web Services requests.
         * Signing a request identifies the sender and prevents the request from being altered.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = Objects.requireNonNull(secretAccessKey, "secretAccessKey cannot be null");
            tracker.setMember($SCHEMA_SECRET_ACCESS_KEY);
            return this;
        }

        /**
         * A part of the temporary security credentials. The session token is used to validate the temporary security
         * credentials.
         *
         * <pre>{@code
         * </p>
         *
         * }</pre>
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder sessionToken(String sessionToken) {
            this.sessionToken = Objects.requireNonNull(sessionToken, "sessionToken cannot be null");
            tracker.setMember($SCHEMA_SESSION_TOKEN);
            return this;
        }

        /**
         * Temporary security credentials expire after a specified interval. After temporary credentials expire, any calls
         * that you make with those credentials will fail. So you must generate a new set of temporary credentials.
         * Temporary credentials cannot be extended or refreshed beyond the original specified interval.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder expiration(Instant expiration) {
            this.expiration = Objects.requireNonNull(expiration, "expiration cannot be null");
            tracker.setMember($SCHEMA_EXPIRATION);
            return this;
        }

        @Override
        public SessionCredentials build() {
            tracker.validate();
            return new SessionCredentials(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> accessKeyId((String) SchemaUtils.validateSameMember($SCHEMA_ACCESS_KEY_ID, member, value));
                case 1 -> secretAccessKey((String) SchemaUtils.validateSameMember($SCHEMA_SECRET_ACCESS_KEY, member, value));
                case 2 -> sessionToken((String) SchemaUtils.validateSameMember($SCHEMA_SESSION_TOKEN, member, value));
                case 3 -> expiration((Instant) SchemaUtils.validateSameMember($SCHEMA_EXPIRATION, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<SessionCredentials> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_ACCESS_KEY_ID)) {
                accessKeyId("");
            }
            if (!tracker.checkMember($SCHEMA_SECRET_ACCESS_KEY)) {
                secretAccessKey("");
            }
            if (!tracker.checkMember($SCHEMA_SESSION_TOKEN)) {
                sessionToken("");
            }
            if (!tracker.checkMember($SCHEMA_EXPIRATION)) {
                expiration(Instant.EPOCH);
            }
            return this;
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.accessKeyId(de.readString(member));
                    case 1 -> builder.secretAccessKey(de.readString(member));
                    case 2 -> builder.sessionToken(de.readString(member));
                    case 3 -> builder.expiration(de.readTimestamp(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
