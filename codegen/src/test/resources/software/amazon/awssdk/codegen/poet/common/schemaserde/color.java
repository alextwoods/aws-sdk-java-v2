package software.amazon.awssdk.services.demo.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SmithyEnum;
import software.amazon.awssdk.utils.internal.EnumUtils;

@Generated("software.amazon.awssdk:codegen")
public enum Color implements SmithyEnum {
  RED("RED"),

  GREEN("GREEN"),

  UNKNOWN_TO_SDK_VERSION(null);

  private static final Map<String, Color> VALUE_MAP = EnumUtils.uniqueIndex(Color.class, Color::toString);

  /**
   * The schema of this enum.
   */
  public static final Schema SCHEMA = DemoSchemas.COLOR;

  private final String value;

  private Color(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  /**
   * Use this in place of valueOf to convert the raw string returned by the service into the enum value.
   *
   * @param value real value
   * @return Color corresponding to the value
   */
  public static Color fromValue(String value) {
    if (value == null) {
      return null;
    }
    return VALUE_MAP.getOrDefault(value, UNKNOWN_TO_SDK_VERSION);
  }

  /**
   * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK.
   * This will return all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
   *
   * @return a {@link Set} of known {@link Color}s
   */
  public static Set<Color> knownValues() {
    Set<Color> knownValues = EnumSet.allOf(Color.class);
    knownValues.remove(UNKNOWN_TO_SDK_VERSION);
    return knownValues;
  }

  /**
   * @return the wire value, or null for {@code UNKNOWN_TO_SDK_VERSION}
   */
  @Override
  public String getValue() {
    return value;
  }

  @Override
  public Schema sdkSchema() {
    return SCHEMA;
  }
}
