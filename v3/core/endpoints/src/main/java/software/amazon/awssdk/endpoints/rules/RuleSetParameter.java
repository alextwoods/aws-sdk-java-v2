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

package software.amazon.awssdk.endpoints.rules;

import java.util.Objects;
import java.util.Optional;

/**
 * One parameter of an endpoint rule set.
 *
 * @param name the parameter name, as referenced by rules and parameter-binding traits
 * @param type the parameter type
 * @param builtIn the built-in the parameter is bound to (for example {@code AWS::Region}), or {@code null}
 * @param defaultValue the value used when nothing is bound (a {@code String}, {@code Boolean} or
 *        {@code List<String>}), or {@code null}
 * @param required whether the rule set declares the parameter required
 * @param documentation the parameter documentation, or {@code null}
 * @param deprecated whether the rule set declares the parameter deprecated
 */
public record RuleSetParameter(String name, Type type, String builtIn, Object defaultValue, boolean required,
                               String documentation, boolean deprecated) {

    /** A rule-set parameter type. */
    public enum Type {
        /** {@code string}: a {@link String}. */
        STRING,
        /** {@code boolean}: a {@link Boolean}. */
        BOOLEAN,
        /** {@code stringArray}: a {@code List<String>}. */
        STRING_ARRAY;

        /** Whether {@code value} is a value of this type. */
        public boolean accepts(Object value) {
            return switch (this) {
                case STRING -> value instanceof String;
                case BOOLEAN -> value instanceof Boolean;
                case STRING_ARRAY -> value instanceof java.util.List<?> list
                                     && list.stream().allMatch(String.class::isInstance);
            };
        }
    }

    public RuleSetParameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }

    /** The built-in the parameter is bound to, if any. */
    public Optional<String> builtInId() {
        return Optional.ofNullable(builtIn);
    }

    /** The value used when nothing is bound, if any. */
    public Optional<Object> defaultValueIfAny() {
        return Optional.ofNullable(defaultValue);
    }
}
