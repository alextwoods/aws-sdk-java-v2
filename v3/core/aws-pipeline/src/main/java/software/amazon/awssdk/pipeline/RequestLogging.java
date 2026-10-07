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

package software.amazon.awssdk.pipeline;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Redaction;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;

/**
 * The per-attempt request log of the pipeline at DEBUG under {@value #LOGGER_NAME}, the V2 request logger name
 * (Requirements 21.8-21.11): the request line, header names and redacted query of every transmitted request, and the
 * status and request IDs of every response. Header values, bodies and query values of sensitive parameters are never
 * rendered: a query parameter is sensitive by {@linkplain Redaction#isSensitiveName name} or when it is bound from a
 * {@code @sensitive} input member, and the path is redacted whole when a {@code @sensitive} member is bound to one of
 * its labels. Nothing is logged at INFO, WARN or ERROR (a failed response reaches the caller as an exception), and no
 * message is built while DEBUG is disabled.
 */
final class RequestLogging {
    /** The V2 {@code SdkStandardLogger.REQUEST_LOGGER} name. */
    static final String LOGGER_NAME = "software.amazon.awssdk.request";

    private static final Logger REQUEST_LOGGER = Logger.loggerFor(LOGGER_NAME);
    private static final String[] REQUEST_ID_HEADERS = {"x-amzn-RequestId", "x-amz-request-id", "x-amzn-request-id"};
    private static final String EXTENDED_REQUEST_ID_HEADER = "x-amz-id-2";

    private RequestLogging() {
    }

    /** Logs the signed request an attempt is about to transmit. */
    static void sending(CallState<?, ?> call, AttemptState attempt, SdkHttpRequest request) {
        if (!REQUEST_LOGGER.isDebugEnabled()) {
            return;
        }
        REQUEST_LOGGER.debug(() -> {
            SensitiveBindings sensitive = SensitiveBindings.of(call.request().schema());
            return "Sending Request: " + request.method() + " " + request.scheme() + "://" + request.authority()
                   + (sensitive.label ? "/" + Redaction.REDACTED : request.encodedPath()) + query(request, sensitive)
                   + " [operation=" + call.plan().operation().id() + ", attempt=" + attempt.number() + ", headers="
                   + headerNames(request.headers()) + "]";
        });
    }

    /** Logs the response an attempt received, successful or not. */
    static void received(CallState<?, ?> call, AttemptState attempt, SdkHttpResponse response) {
        if (!REQUEST_LOGGER.isDebugEnabled()) {
            return;
        }
        REQUEST_LOGGER.debug(() -> {
            int status = response.statusCode();
            HttpHeaders headers = response.headers();
            return (status >= 200 && status < 300 ? "Received successful response: " : "Received failed response: ")
                   + status + ", Request ID: " + requestId(headers) + ", Extended Request ID: "
                   + headers.firstValue(EXTENDED_REQUEST_ID_HEADER).orElse("not available") + " [operation="
                   + call.plan().operation().id() + ", attempt=" + attempt.number() + "]";
        });
    }

    private static String query(SdkHttpRequest request, SensitiveBindings sensitive) {
        String encoded = request.encodedQuery();
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        if (!sensitive.allQuery() && sensitive.queryNames().isEmpty()) {
            return "?" + Redaction.query(encoded);
        }
        StringJoiner out = new StringJoiner("&", "?", "");
        for (String pair : encoded.split("&", -1)) {
            int equals = pair.indexOf('=');
            boolean bound = equals >= 0
                            && (sensitive.allQuery() || sensitive.queryNames().contains(decode(pair.substring(0, equals))));
            if (bound) {
                out.add(pair.substring(0, equals + 1) + Redaction.REDACTED);
            } else {
                out.add(Redaction.query(pair));
            }
        }
        return out.toString();
    }

    private static String decode(String encodedName) {
        try {
            return URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return encodedName;
        }
    }

    /**
     * Where an input's {@code @sensitive} members are bound in the request target: whether one is a path label, the
     * names of the query parameters bound from them, and whether a sensitive {@code httpQueryParams} map may supply
     * any parameter. Members of protocols that ignore HTTP binding traits carry none of these traits.
     */
    private record SensitiveBindings(boolean label, boolean allQuery, Set<String> queryNames) {
        static SensitiveBindings of(Schema input) {
            boolean label = false;
            boolean allQuery = false;
            Set<String> queryNames = new HashSet<>();
            for (Schema member : input.members()) {
                if (!revealsSensitive(member)) {
                    continue;
                }
                label |= member.hasTrait(TraitKey.HTTP_LABEL_TRAIT);
                allQuery |= member.hasTrait(TraitKey.HTTP_QUERY_PARAMS_TRAIT);
                if (member.hasTrait(TraitKey.HTTP_QUERY_TRAIT)) {
                    queryNames.add(member.getTrait(TraitKey.HTTP_QUERY_TRAIT).getValue());
                }
            }
            return new SensitiveBindings(label, allQuery, queryNames);
        }

        /** Whether the value of {@code schema} is, or contains as a list element or map key or value, sensitive. */
        private static boolean revealsSensitive(Schema schema) {
            if (schema.hasTrait(TraitKey.SENSITIVE_TRAIT)
                || schema.isMember() && schema.memberTarget().hasTrait(TraitKey.SENSITIVE_TRAIT)) {
                return true;
            }
            Schema target = schema.isMember() ? schema.memberTarget() : schema;
            return switch (target.type()) {
                case LIST, SET -> revealsSensitive(target.listMember());
                case MAP -> revealsSensitive(target.mapKeyMember()) || revealsSensitive(target.mapValueMember());
                default -> false;
            };
        }
    }

    private static String requestId(HttpHeaders headers) {
        for (String name : REQUEST_ID_HEADERS) {
            String value = headers.firstValue(name).orElse(null);
            if (value != null) {
                return value;
            }
        }
        return "not available";
    }

    private static String headerNames(HttpHeaders headers) {
        StringBuilder names = new StringBuilder("[");
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(headers.name(i));
        }
        return names.append(']').toString();
    }
}
