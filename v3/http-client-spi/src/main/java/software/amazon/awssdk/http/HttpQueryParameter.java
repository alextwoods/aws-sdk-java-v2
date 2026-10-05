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

package software.amazon.awssdk.http;

/**
 * One raw query entry of an {@link SdkHttpRequest}: the name and value exactly as they appear on the wire, already
 * percent-encoded (Requirement 12.2). Transports and signers use the raw text as is; nothing re-encodes it.
 *
 * <p>Create an entry from decoded text with {@link #of(String, String)} (encoded once, RFC 3986 unreserved set) or
 * from text a protocol already encoded with {@link #raw(String, String)} (validated, never changed).
 *
 * @param rawName the encoded name; non-empty, without {@code &}, {@code =} or {@code #}
 * @param rawValue the encoded value without {@code &} or {@code #}, or {@code null} for an entry rendered as its name
 *     only (an empty value renders as {@code name=})
 */
public record HttpQueryParameter(String rawName, String rawValue) {
    public HttpQueryParameter {
        RawTargets.requireQueryComponent(rawName, true);
        if (rawValue != null) {
            RawTargets.requireQueryComponent(rawValue, false);
        }
    }

    /** An entry of the decoded {@code name} and {@code value} ({@code null} for name only), each encoded once. */
    public static HttpQueryParameter of(String name, String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        return new HttpQueryParameter(UriEncoding.encode(name), value == null ? null : UriEncoding.encode(value));
    }

    /** An entry of already encoded text, kept exactly. */
    public static HttpQueryParameter raw(String rawName, String rawValue) {
        return new HttpQueryParameter(rawName, rawValue);
    }

    /** {@code rawName}, or {@code rawName=rawValue}: the entry as it appears in the query string. */
    public String render() {
        return rawValue == null ? rawName : rawName + "=" + rawValue;
    }

    /** Renders the name only; values may be sensitive. */
    @Override
    public String toString() {
        return "HttpQueryParameter[" + rawName + "]";
    }
}
