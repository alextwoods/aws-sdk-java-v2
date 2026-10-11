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

package software.amazon.awssdk.protocol.smithytests;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.utils.Validate;

/**
 * One decoded {@code name[=value]} pair from a query string or an {@code application/x-www-form-urlencoded} body.
 *
 * <p>A pair written without {@code =} has a null value; a pair written as {@code name=} has an empty value. Decoding is
 * RFC 3986 percent-decoding only: {@code +} stays a literal plus sign, so a serializer that writes {@code +} for a space is
 * detected.
 */
public final class QueryParameter {
    private final String name;
    private final String value;

    private QueryParameter(String name, String value) {
        this.name = Validate.paramNotNull(name, "name");
        this.value = value;
    }

    /**
     * Create a pair from already-decoded parts. {@code value} may be null.
     */
    public static QueryParameter of(String name, String value) {
        return new QueryParameter(name, value);
    }

    /**
     * Parse one percent-encoded {@code name[=value]} pair, as written in the Smithy {@code queryParams} list.
     */
    public static QueryParameter parse(String encodedPair) {
        Validate.paramNotNull(encodedPair, "encodedPair");
        int eq = encodedPair.indexOf('=');
        if (eq < 0) {
            return new QueryParameter(PercentDecoding.decode(encodedPair), null);
        }
        return new QueryParameter(PercentDecoding.decode(encodedPair.substring(0, eq)),
                                  PercentDecoding.decode(encodedPair.substring(eq + 1)));
    }

    /**
     * Parse every {@code &}-separated pair of a percent-encoded query string or form body. Empty segments are ignored, and
     * a null or empty input yields an empty list.
     */
    public static List<QueryParameter> parseAll(String encoded) {
        List<QueryParameter> result = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return result;
        }
        for (String pair : encoded.split("&", -1)) {
            if (!pair.isEmpty()) {
                result.add(parse(pair));
            }
        }
        return result;
    }

    public String name() {
        return name;
    }

    /**
     * The decoded value, or null when the pair had no {@code =}.
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryParameter)) {
            return false;
        }
        QueryParameter that = (QueryParameter) o;
        return name.equals(that.name) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value == null ? name : name + "=" + value;
    }
}
