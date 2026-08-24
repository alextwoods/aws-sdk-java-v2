package software.amazon.awssdk.benchmark.smithyjava.s3.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.SmithyEnum;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public sealed interface BucketLocationConstraint extends SmithyEnum, SerializableShape {
    BucketLocationConstraint AF_SOUTH_1 = new AfSouth1Type();
    BucketLocationConstraint AP_EAST_1 = new ApEast1Type();
    BucketLocationConstraint AP_EAST_2 = new ApEast2Type();
    BucketLocationConstraint AP_NORTHEAST_1 = new ApNortheast1Type();
    BucketLocationConstraint AP_NORTHEAST_2 = new ApNortheast2Type();
    BucketLocationConstraint AP_NORTHEAST_3 = new ApNortheast3Type();
    BucketLocationConstraint AP_SOUTH_1 = new ApSouth1Type();
    BucketLocationConstraint AP_SOUTH_2 = new ApSouth2Type();
    BucketLocationConstraint AP_SOUTHEAST_1 = new ApSoutheast1Type();
    BucketLocationConstraint AP_SOUTHEAST_2 = new ApSoutheast2Type();
    BucketLocationConstraint AP_SOUTHEAST_3 = new ApSoutheast3Type();
    BucketLocationConstraint AP_SOUTHEAST_4 = new ApSoutheast4Type();
    BucketLocationConstraint AP_SOUTHEAST_5 = new ApSoutheast5Type();
    BucketLocationConstraint AP_SOUTHEAST_6 = new ApSoutheast6Type();
    BucketLocationConstraint AP_SOUTHEAST_7 = new ApSoutheast7Type();
    BucketLocationConstraint CA_CENTRAL_1 = new CaCentral1Type();
    BucketLocationConstraint CA_WEST_1 = new CaWest1Type();
    BucketLocationConstraint CN_NORTH_1 = new CnNorth1Type();
    BucketLocationConstraint CN_NORTHWEST_1 = new CnNorthwest1Type();
    BucketLocationConstraint EU = new EuType();
    BucketLocationConstraint EU_CENTRAL_1 = new EuCentral1Type();
    BucketLocationConstraint EU_CENTRAL_2 = new EuCentral2Type();
    BucketLocationConstraint EU_NORTH_1 = new EuNorth1Type();
    BucketLocationConstraint EU_SOUTH_1 = new EuSouth1Type();
    BucketLocationConstraint EU_SOUTH_2 = new EuSouth2Type();
    BucketLocationConstraint EU_WEST_1 = new EuWest1Type();
    BucketLocationConstraint EU_WEST_2 = new EuWest2Type();
    BucketLocationConstraint EU_WEST_3 = new EuWest3Type();
    BucketLocationConstraint IL_CENTRAL_1 = new IlCentral1Type();
    BucketLocationConstraint ME_CENTRAL_1 = new MeCentral1Type();
    BucketLocationConstraint ME_SOUTH_1 = new MeSouth1Type();
    BucketLocationConstraint MX_CENTRAL_1 = new MxCentral1Type();
    BucketLocationConstraint SA_EAST_1 = new SaEast1Type();
    BucketLocationConstraint US_EAST_2 = new UsEast2Type();
    BucketLocationConstraint US_GOV_EAST_1 = new UsGovEast1Type();
    BucketLocationConstraint US_GOV_WEST_1 = new UsGovWest1Type();
    BucketLocationConstraint US_WEST_1 = new UsWest1Type();
    BucketLocationConstraint US_WEST_2 = new UsWest2Type();
    List<BucketLocationConstraint> $TYPES = List.of(AF_SOUTH_1, AP_EAST_1, AP_EAST_2, AP_NORTHEAST_1, AP_NORTHEAST_2, AP_NORTHEAST_3, AP_SOUTH_1, AP_SOUTH_2, AP_SOUTHEAST_1, AP_SOUTHEAST_2, AP_SOUTHEAST_3, AP_SOUTHEAST_4, AP_SOUTHEAST_5, AP_SOUTHEAST_6, AP_SOUTHEAST_7, CA_CENTRAL_1, CA_WEST_1, CN_NORTH_1, CN_NORTHWEST_1, EU, EU_CENTRAL_1, EU_CENTRAL_2, EU_NORTH_1, EU_SOUTH_1, EU_SOUTH_2, EU_WEST_1, EU_WEST_2, EU_WEST_3, IL_CENTRAL_1, ME_CENTRAL_1, ME_SOUTH_1, MX_CENTRAL_1, SA_EAST_1, US_EAST_2, US_GOV_EAST_1, US_GOV_WEST_1, US_WEST_1, US_WEST_2);

    Schema $SCHEMA = Schema.createEnum(ShapeId.from("com.amazonaws.s3#BucketLocationConstraint"),
        Set.of(AF_SOUTH_1.getValue(), AP_EAST_1.getValue(), AP_EAST_2.getValue(), AP_NORTHEAST_1.getValue(), AP_NORTHEAST_2.getValue(), AP_NORTHEAST_3.getValue(), AP_SOUTH_1.getValue(), AP_SOUTH_2.getValue(), AP_SOUTHEAST_1.getValue(), AP_SOUTHEAST_2.getValue(), AP_SOUTHEAST_3.getValue(), AP_SOUTHEAST_4.getValue(), AP_SOUTHEAST_5.getValue(), AP_SOUTHEAST_6.getValue(), AP_SOUTHEAST_7.getValue(), CA_CENTRAL_1.getValue(), CA_WEST_1.getValue(), CN_NORTH_1.getValue(), CN_NORTHWEST_1.getValue(), EU.getValue(), EU_CENTRAL_1.getValue(), EU_CENTRAL_2.getValue(), EU_NORTH_1.getValue(), EU_SOUTH_1.getValue(), EU_SOUTH_2.getValue(), EU_WEST_1.getValue(), EU_WEST_2.getValue(), EU_WEST_3.getValue(), IL_CENTRAL_1.getValue(), ME_CENTRAL_1.getValue(), ME_SOUTH_1.getValue(), MX_CENTRAL_1.getValue(), SA_EAST_1.getValue(), US_EAST_2.getValue(), US_GOV_EAST_1.getValue(), US_GOV_WEST_1.getValue(), US_WEST_1.getValue(), US_WEST_2.getValue()), BucketLocationConstraint.class
    );

    ShapeId $ID = $SCHEMA.id();

    String getValue();

    @Override
    default void serialize(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA, getValue());
    }

    /**
     * Create an unknown enum variant with the given value.
     *
     * @param value value for the unknown variant.
     */
    static BucketLocationConstraint unknown(String value) {
        return new $Unknown(value);
    }

    /**
     * Returns an unmodifiable list containing the constants of this enum type, in the order declared.
     */
    static List<BucketLocationConstraint> values() {
        return $TYPES;
    }

    /**
     * Returns a {@link BucketLocationConstraint} constant with the specified value.
     *
     * @param value value to create {@code BucketLocationConstraint} from.
     * @throws IllegalArgumentException if value does not match a known value.
     */
    static BucketLocationConstraint from(String value) {
        return switch (value) {
            case "af-south-1" -> AF_SOUTH_1;
            case "ap-east-1" -> AP_EAST_1;
            case "ap-east-2" -> AP_EAST_2;
            case "ap-northeast-1" -> AP_NORTHEAST_1;
            case "ap-northeast-2" -> AP_NORTHEAST_2;
            case "ap-northeast-3" -> AP_NORTHEAST_3;
            case "ap-south-1" -> AP_SOUTH_1;
            case "ap-south-2" -> AP_SOUTH_2;
            case "ap-southeast-1" -> AP_SOUTHEAST_1;
            case "ap-southeast-2" -> AP_SOUTHEAST_2;
            case "ap-southeast-3" -> AP_SOUTHEAST_3;
            case "ap-southeast-4" -> AP_SOUTHEAST_4;
            case "ap-southeast-5" -> AP_SOUTHEAST_5;
            case "ap-southeast-6" -> AP_SOUTHEAST_6;
            case "ap-southeast-7" -> AP_SOUTHEAST_7;
            case "ca-central-1" -> CA_CENTRAL_1;
            case "ca-west-1" -> CA_WEST_1;
            case "cn-north-1" -> CN_NORTH_1;
            case "cn-northwest-1" -> CN_NORTHWEST_1;
            case "EU" -> EU;
            case "eu-central-1" -> EU_CENTRAL_1;
            case "eu-central-2" -> EU_CENTRAL_2;
            case "eu-north-1" -> EU_NORTH_1;
            case "eu-south-1" -> EU_SOUTH_1;
            case "eu-south-2" -> EU_SOUTH_2;
            case "eu-west-1" -> EU_WEST_1;
            case "eu-west-2" -> EU_WEST_2;
            case "eu-west-3" -> EU_WEST_3;
            case "il-central-1" -> IL_CENTRAL_1;
            case "me-central-1" -> ME_CENTRAL_1;
            case "me-south-1" -> ME_SOUTH_1;
            case "mx-central-1" -> MX_CENTRAL_1;
            case "sa-east-1" -> SA_EAST_1;
            case "us-east-2" -> US_EAST_2;
            case "us-gov-east-1" -> US_GOV_EAST_1;
            case "us-gov-west-1" -> US_GOV_WEST_1;
            case "us-west-1" -> US_WEST_1;
            case "us-west-2" -> US_WEST_2;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

    final class AfSouth1Type implements BucketLocationConstraint {
        private AfSouth1Type() {}

        @Override
        public String getValue() {
            return "af-south-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApEast1Type implements BucketLocationConstraint {
        private ApEast1Type() {}

        @Override
        public String getValue() {
            return "ap-east-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApEast2Type implements BucketLocationConstraint {
        private ApEast2Type() {}

        @Override
        public String getValue() {
            return "ap-east-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApNortheast1Type implements BucketLocationConstraint {
        private ApNortheast1Type() {}

        @Override
        public String getValue() {
            return "ap-northeast-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApNortheast2Type implements BucketLocationConstraint {
        private ApNortheast2Type() {}

        @Override
        public String getValue() {
            return "ap-northeast-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApNortheast3Type implements BucketLocationConstraint {
        private ApNortheast3Type() {}

        @Override
        public String getValue() {
            return "ap-northeast-3";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSouth1Type implements BucketLocationConstraint {
        private ApSouth1Type() {}

        @Override
        public String getValue() {
            return "ap-south-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSouth2Type implements BucketLocationConstraint {
        private ApSouth2Type() {}

        @Override
        public String getValue() {
            return "ap-south-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSoutheast1Type implements BucketLocationConstraint {
        private ApSoutheast1Type() {}

        @Override
        public String getValue() {
            return "ap-southeast-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSoutheast2Type implements BucketLocationConstraint {
        private ApSoutheast2Type() {}

        @Override
        public String getValue() {
            return "ap-southeast-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSoutheast3Type implements BucketLocationConstraint {
        private ApSoutheast3Type() {}

        @Override
        public String getValue() {
            return "ap-southeast-3";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSoutheast4Type implements BucketLocationConstraint {
        private ApSoutheast4Type() {}

        @Override
        public String getValue() {
            return "ap-southeast-4";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSoutheast5Type implements BucketLocationConstraint {
        private ApSoutheast5Type() {}

        @Override
        public String getValue() {
            return "ap-southeast-5";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSoutheast6Type implements BucketLocationConstraint {
        private ApSoutheast6Type() {}

        @Override
        public String getValue() {
            return "ap-southeast-6";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class ApSoutheast7Type implements BucketLocationConstraint {
        private ApSoutheast7Type() {}

        @Override
        public String getValue() {
            return "ap-southeast-7";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CaCentral1Type implements BucketLocationConstraint {
        private CaCentral1Type() {}

        @Override
        public String getValue() {
            return "ca-central-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CaWest1Type implements BucketLocationConstraint {
        private CaWest1Type() {}

        @Override
        public String getValue() {
            return "ca-west-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CnNorth1Type implements BucketLocationConstraint {
        private CnNorth1Type() {}

        @Override
        public String getValue() {
            return "cn-north-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class CnNorthwest1Type implements BucketLocationConstraint {
        private CnNorthwest1Type() {}

        @Override
        public String getValue() {
            return "cn-northwest-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuType implements BucketLocationConstraint {
        private EuType() {}

        @Override
        public String getValue() {
            return "EU";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuCentral1Type implements BucketLocationConstraint {
        private EuCentral1Type() {}

        @Override
        public String getValue() {
            return "eu-central-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuCentral2Type implements BucketLocationConstraint {
        private EuCentral2Type() {}

        @Override
        public String getValue() {
            return "eu-central-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuNorth1Type implements BucketLocationConstraint {
        private EuNorth1Type() {}

        @Override
        public String getValue() {
            return "eu-north-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuSouth1Type implements BucketLocationConstraint {
        private EuSouth1Type() {}

        @Override
        public String getValue() {
            return "eu-south-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuSouth2Type implements BucketLocationConstraint {
        private EuSouth2Type() {}

        @Override
        public String getValue() {
            return "eu-south-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuWest1Type implements BucketLocationConstraint {
        private EuWest1Type() {}

        @Override
        public String getValue() {
            return "eu-west-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuWest2Type implements BucketLocationConstraint {
        private EuWest2Type() {}

        @Override
        public String getValue() {
            return "eu-west-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class EuWest3Type implements BucketLocationConstraint {
        private EuWest3Type() {}

        @Override
        public String getValue() {
            return "eu-west-3";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class IlCentral1Type implements BucketLocationConstraint {
        private IlCentral1Type() {}

        @Override
        public String getValue() {
            return "il-central-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MeCentral1Type implements BucketLocationConstraint {
        private MeCentral1Type() {}

        @Override
        public String getValue() {
            return "me-central-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MeSouth1Type implements BucketLocationConstraint {
        private MeSouth1Type() {}

        @Override
        public String getValue() {
            return "me-south-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class MxCentral1Type implements BucketLocationConstraint {
        private MxCentral1Type() {}

        @Override
        public String getValue() {
            return "mx-central-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class SaEast1Type implements BucketLocationConstraint {
        private SaEast1Type() {}

        @Override
        public String getValue() {
            return "sa-east-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UsEast2Type implements BucketLocationConstraint {
        private UsEast2Type() {}

        @Override
        public String getValue() {
            return "us-east-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UsGovEast1Type implements BucketLocationConstraint {
        private UsGovEast1Type() {}

        @Override
        public String getValue() {
            return "us-gov-east-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UsGovWest1Type implements BucketLocationConstraint {
        private UsGovWest1Type() {}

        @Override
        public String getValue() {
            return "us-gov-west-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UsWest1Type implements BucketLocationConstraint {
        private UsWest1Type() {}

        @Override
        public String getValue() {
            return "us-west-1";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    final class UsWest2Type implements BucketLocationConstraint {
        private UsWest2Type() {}

        @Override
        public String getValue() {
            return "us-west-2";
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String value) implements BucketLocationConstraint {
        public $Unknown {
            Objects.requireNonNull(value, "Value cannot be null");
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

        private final class $Hidden implements BucketLocationConstraint {
            @Override
            public String getValue() {
                return null;
            }
        }
    }

    /**
     * @return returns a new Builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link BucketLocationConstraint}.
     */
    final class Builder implements ShapeBuilder<BucketLocationConstraint> {
        private String value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        private Builder value(String value) {
            this.value = Objects.requireNonNull(value, "Enum value cannot be null");
            return this;
        }

        @Override
        public BucketLocationConstraint build() {
            return switch (value) {
                case "af-south-1" -> AF_SOUTH_1;
                case "ap-east-1" -> AP_EAST_1;
                case "ap-east-2" -> AP_EAST_2;
                case "ap-northeast-1" -> AP_NORTHEAST_1;
                case "ap-northeast-2" -> AP_NORTHEAST_2;
                case "ap-northeast-3" -> AP_NORTHEAST_3;
                case "ap-south-1" -> AP_SOUTH_1;
                case "ap-south-2" -> AP_SOUTH_2;
                case "ap-southeast-1" -> AP_SOUTHEAST_1;
                case "ap-southeast-2" -> AP_SOUTHEAST_2;
                case "ap-southeast-3" -> AP_SOUTHEAST_3;
                case "ap-southeast-4" -> AP_SOUTHEAST_4;
                case "ap-southeast-5" -> AP_SOUTHEAST_5;
                case "ap-southeast-6" -> AP_SOUTHEAST_6;
                case "ap-southeast-7" -> AP_SOUTHEAST_7;
                case "ca-central-1" -> CA_CENTRAL_1;
                case "ca-west-1" -> CA_WEST_1;
                case "cn-north-1" -> CN_NORTH_1;
                case "cn-northwest-1" -> CN_NORTHWEST_1;
                case "EU" -> EU;
                case "eu-central-1" -> EU_CENTRAL_1;
                case "eu-central-2" -> EU_CENTRAL_2;
                case "eu-north-1" -> EU_NORTH_1;
                case "eu-south-1" -> EU_SOUTH_1;
                case "eu-south-2" -> EU_SOUTH_2;
                case "eu-west-1" -> EU_WEST_1;
                case "eu-west-2" -> EU_WEST_2;
                case "eu-west-3" -> EU_WEST_3;
                case "il-central-1" -> IL_CENTRAL_1;
                case "me-central-1" -> ME_CENTRAL_1;
                case "me-south-1" -> ME_SOUTH_1;
                case "mx-central-1" -> MX_CENTRAL_1;
                case "sa-east-1" -> SA_EAST_1;
                case "us-east-2" -> US_EAST_2;
                case "us-gov-east-1" -> US_GOV_EAST_1;
                case "us-gov-west-1" -> US_GOV_WEST_1;
                case "us-west-1" -> US_WEST_1;
                case "us-west-2" -> US_WEST_2;
                default -> new $Unknown(value);
            };
        }

        @Override
        public Builder deserialize(ShapeDeserializer de) {
            return value(de.readString($SCHEMA));
        }
    }
}
