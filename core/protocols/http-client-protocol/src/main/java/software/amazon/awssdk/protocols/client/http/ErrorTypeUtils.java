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

package software.amazon.awssdk.protocols.client.http;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeIdSyntaxException;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Helpers that read an error discriminator (the error type or code a service sends) and resolve it to the shape ID of a
 * modeled error.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.http.ErrorTypeUtils}. The SDK adds
 * {@link #resolve(String, String, TypeRegistry, boolean)}, which gathers the normalization rules every protocol uses,
 * {@link #readMessage(Document)} and {@link #queryErrorCode(String)}.
 */
@SdkPublicApi
public final class ErrorTypeUtils {
    private static final String[] MESSAGE_MEMBERS = {"message", "Message", "errorMessage"};

    private ErrorTypeUtils() {
    }

    /**
     * Reads the error type from the {@code __type} member of a document.
     *
     * @param document the document of the error payload; may be null
     * @return the value of {@code __type}, or null if the document is not a map or structure, or has no string
     *     {@code __type} member
     */
    public static String readType(Document document) {
        return readString(document, "__type");
    }

    /**
     * Reads the error type from the {@code __type} member of a document or, if it has none, from its {@code code}
     * member.
     *
     * @param document the document of the error payload; may be null
     * @return the error type, or null if neither member is a string
     */
    public static String readTypeAndCode(Document document) {
        String errorType = readType(document);
        return errorType != null ? errorType : readString(document, "code");
    }

    /**
     * Reads the error message from the first of the {@code message}, {@code Message} and {@code errorMessage} members of
     * a document that is a string, in that order, as earlier SDK clients do for JSON errors.
     *
     * @param document the document of the error payload; may be null
     * @return the error message, or null if none of the members is a string
     */
    public static String readMessage(Document document) {
        for (String member : MESSAGE_MEMBERS) {
            String message = readString(document, member);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    /**
     * Removes the trailing URI from an error type.
     *
     * <p>For example, given {@code aws.protocoltests.restjson#FooError:http://abc.com}, protocols such as restJson1
     * ignore the trailing URI and keep the namespace of the error type.
     *
     * @param text the error type; may be null
     * @return the error type without anything from its first {@code :} onward, or null if {@code text} is null
     */
    public static String removeUri(String text) {
        if (text == null) {
            return null;
        }
        int colon = text.indexOf(':');
        return colon > 0 ? text.substring(0, colon) : text;
    }

    /**
     * Removes the namespace and the trailing URI from an error type.
     *
     * <p>For example, given {@code aws.protocoltests.restjson#FooError:http://abc.com}, this returns {@code FooError}.
     * This is the error code earlier SDK clients report for JSON protocols.
     *
     * @param text the error type; may be null
     * @return the error type without its namespace and trailing URI, or null if {@code text} is null
     */
    public static String removeNamespaceAndUri(String text) {
        if (text == null) {
            return null;
        }
        int hash = text.indexOf('#');
        if (hash > 0) {
            text = text.substring(hash + 1);
        }
        return removeUri(text);
    }

    /**
     * Extracts the error code from the value of an {@code x-amzn-query-error} header, which services with the
     * {@code aws.protocols#awsQueryCompatible} trait send as {@code <code>;<fault>}.
     *
     * @param headerValue the header value; may be null
     * @return the text before the first {@code ;}, or null if the value is null or has no non-empty code before a
     *     {@code ;}
     */
    public static String queryErrorCode(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        int delimiter = headerValue.indexOf(';');
        return delimiter > 0 ? headerValue.substring(0, delimiter) : null;
    }

    /**
     * Resolves an error discriminator to the shape ID of an error in a registry. Only shape IDs the registry
     * {@linkplain TypeRegistry#contains(ShapeId) contains} are returned; no error class is loaded.
     *
     * <p>The rules are applied in this order:
     * <ol>
     *     <li>If {@code useErrorCodeIndex} is true and the registry is an {@link ErrorRegistry}, the raw value is looked
     *     up with {@link ErrorRegistry#resolveCode(String)}, which holds the custom codes of the
     *     {@code aws.protocols#awsQueryError} trait. This is for the awsQuery, ec2Query and awsQueryCompatible
     *     protocols.</li>
     *     <li>Anything from the first {@code :} onward is dropped.</li>
     *     <li>A value containing {@code #} is an absolute shape ID and is matched exactly.</li>
     *     <li>Otherwise, or if the absolute ID is not in the registry, the shape name (the part after any {@code #}) is
     *     resolved in the service namespace.</li>
     * </ol>
     *
     * @param errorType the discriminator read from the response; may be null
     * @param serviceNamespace the namespace of the service, used for relative values; may be null, in which case only
     *     absolute values and custom codes resolve
     * @param registry the registry of the errors that can be created; may be null
     * @param useErrorCodeIndex whether to look the value up as an {@code awsQueryError} custom code first
     * @return the resolved shape ID, or null if the value is null, empty, not a valid shape ID, or not in the registry
     */
    public static ShapeId resolve(String errorType, String serviceNamespace, TypeRegistry registry,
                                  boolean useErrorCodeIndex) {
        if (errorType == null || errorType.isEmpty() || registry == null) {
            return null;
        }
        if (useErrorCodeIndex && registry instanceof ErrorRegistry) {
            ShapeId id = ((ErrorRegistry) registry).resolveCode(errorType);
            if (id != null && registry.contains(id)) {
                return id;
            }
        }

        String type = removeUri(errorType);
        int hash = type.indexOf('#');
        if (hash >= 0) {
            ShapeId absolute = parse(type);
            if (absolute != null && registry.contains(absolute)) {
                return absolute;
            }
            type = type.substring(hash + 1);
        }
        if (serviceNamespace == null || type.isEmpty()) {
            return null;
        }
        ShapeId relative = parse(serviceNamespace + "#" + type);
        return relative != null && registry.contains(relative) ? relative : null;
    }

    private static ShapeId parse(String absoluteId) {
        try {
            ShapeId id = ShapeId.from(absoluteId);
            // A member ID ("ns#Name$member") never names an error shape.
            return id.hasMember() ? null : id;
        } catch (ShapeIdSyntaxException e) {
            return null;
        }
    }

    private static String readString(Document document, String member) {
        if (document == null) {
            return null;
        }
        ShapeType type = document.type();
        if (type != ShapeType.MAP && type != ShapeType.STRUCTURE && type != ShapeType.UNION) {
            return null;
        }
        Document value = document.getMember(member);
        return value != null && value.isType(ShapeType.STRING) ? value.asString() : null;
    }
}
