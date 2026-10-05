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

package software.amazon.awssdk.endpoints.rules.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The algorithms of the endpoint rules library functions, matching the canonical Smithy rules evaluator (Smithy
 * {@code smithy-rules-engine} and {@code smithy-aws-endpoints}) character for character. A function result of
 * {@code null} is the rules' empty value.
 */
public final class RulesFunctions {
    private RulesFunctions() {
    }

    /**
     * {@code substring(input, start, stop, reverse)}: the characters {@code [start, stop)}, counted from the end when
     * {@code reverse}; empty if the range is invalid or the input has a non-ASCII character.
     */
    public static String substring(String value, int startIndex, int stopIndex, boolean reverse) {
        int length = value.length();
        if (startIndex >= stopIndex || length < stopIndex) {
            return null;
        }
        for (int i = 0; i < length; i++) {
            if (value.charAt(i) > 127) {
                return null;
            }
        }
        return reverse ? value.substring(length - stopIndex, length - startIndex)
                       : value.substring(startIndex, stopIndex);
    }

    /** {@code uriEncode(value)}: RFC 3986 percent-encoding of every character outside the unreserved set. */
    public static String uriEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                         .replace("+", "%20")
                         .replace("*", "%2A")
                         .replace("%7E", "~");
    }

    /**
     * {@code isValidHostLabel(value, allowSubDomains)}: whether the value is one RFC 1123 host label (letters, digits
     * and hyphens, 1 to 63 characters, not starting with a hyphen), or with {@code allowDots} a dot-separated
     * sequence of them.
     */
    public static boolean isValidHostLabel(String value, boolean allowDots) {
        int length = value.length();
        if (length == 0) {
            return false;
        }
        if (!allowDots) {
            return isValidSingleLabel(value, 0, length);
        }
        int start = 0;
        for (int i = 0; i <= length; i++) {
            if (i == length || value.charAt(i) == '.') {
                if (!isValidSingleLabel(value, start, i)) {
                    return false;
                }
                start = i + 1;
            }
        }
        return true;
    }

    private static boolean isValidSingleLabel(String s, int start, int end) {
        int length = end - start;
        if (length < 1 || length > 63 || !isAsciiAlphanumeric(s.charAt(start))) {
            return false;
        }
        for (int i = start + 1; i < end; i++) {
            char c = s.charAt(i);
            if (!isAsciiAlphanumeric(c) && c != '-') {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiAlphanumeric(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    /**
     * {@code parseURL(value)}: the record {@code {scheme, authority, path, normalizedPath, isIp}} of a URL without a
     * query string; empty if the value is not a URL or has a query.
     */
    @SuppressWarnings("deprecation") // java.net.URL(String) is the canonical evaluator's parser.
    public static Map<String, Object> parseUrl(String value) {
        URL url;
        try {
            url = new URL(value);
        } catch (MalformedURLException e) {
            return null;
        }
        if (url.getQuery() != null) {
            return null;
        }
        String path = url.getPath();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scheme", url.getProtocol());
        result.put("authority", url.getAuthority());
        result.put("path", path);
        result.put("normalizedPath", normalizePath(path));
        result.put("isIp", isIpAddress(url.getHost()));
        return Collections.unmodifiableMap(result);
    }

    /** The path with exactly one leading and one trailing slash added where missing; {@code /} if blank. */
    static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        boolean leading = path.startsWith("/");
        boolean trailing = path.endsWith("/");
        return (leading ? "" : "/") + path + (trailing ? "" : "/");
    }

    /**
     * Whether {@code host} is an IP address: a bracketed IPv6 literal, or four dot-separated decimal segments of one
     * to three digits without leading zeros (segment values are intentionally not range-checked).
     */
    public static boolean isIpAddress(String host) {
        if (host == null || host.length() < 2) {
            return false;
        }
        if (host.charAt(0) == '[' && host.charAt(host.length() - 1) == ']') {
            return true;
        }
        int from = 0;
        int segments = 0;
        boolean done = false;
        while (!done) {
            int index = host.indexOf('.', from);
            if (index == -1) {
                if (segments != 3) {
                    return false;
                }
                index = host.length();
                done = true;
            } else if (segments == 3) {
                return false;
            }
            int length = index - from;
            if (length < 1 || length > 3) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                char c = host.charAt(from + i);
                boolean leadingOfMany = i == 0 && length > 1;
                if ((leadingOfMany ? c <= '0' : c < '0') || c > '9') {
                    return false;
                }
            }
            from = index + 1;
            segments++;
        }
        return true;
    }

    /**
     * {@code split(value, delimiter, limit)}: the value split on every occurrence of {@code delimiter}, into at most
     * {@code limit} parts (unlimited when 0).
     */
    public static List<String> split(String value, String delimiter, int limit) {
        if (delimiter.isEmpty()) {
            throw new RuleEvaluationError("split delimiter cannot be empty");
        }
        if (limit < 0) {
            throw new RuleEvaluationError("split limit cannot be negative, but given: " + limit);
        }
        if (value.isEmpty() || limit == 1) {
            return List.of(value);
        }
        List<String> result = new ArrayList<>();
        int start = 0;
        int splits = 0;
        while (limit == 0 || splits < limit - 1) {
            int position = value.indexOf(delimiter, start);
            if (position == -1) {
                break;
            }
            result.add(value.substring(start, position));
            start = position + delimiter.length();
            splits++;
        }
        result.add(value.substring(start));
        return result;
    }

    /**
     * {@code aws.parseArn(value)}: the record {@code {partition, service, region, accountId, resourceId}} of an ARN,
     * where {@code resourceId} is the resource split on {@code :} and {@code /}; empty if the value is not an ARN.
     */
    public static Map<String, Object> parseArn(String arn) {
        if (arn.length() < 8 || !arn.startsWith("arn:")) {
            return null;
        }
        int p0 = 3;
        int p1 = arn.indexOf(':', p0 + 1);
        int p2 = p1 < 0 ? -1 : arn.indexOf(':', p1 + 1);
        int p3 = p2 < 0 ? -1 : arn.indexOf(':', p2 + 1);
        int p4 = p3 < 0 ? -1 : arn.indexOf(':', p3 + 1);
        if (p4 < 0) {
            return null;
        }
        String partition = arn.substring(p0 + 1, p1);
        String service = arn.substring(p1 + 1, p2);
        String region = arn.substring(p2 + 1, p3);
        String accountId = arn.substring(p3 + 1, p4);
        String resource = arn.substring(p4 + 1);
        if (partition.isEmpty() || service.isEmpty() || resource.isEmpty()) {
            return null;
        }
        List<Object> resourceId = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < resource.length(); i++) {
            char c = resource.charAt(i);
            if (c == ':' || c == '/') {
                resourceId.add(resource.substring(start, i));
                start = i + 1;
            }
        }
        resourceId.add(resource.substring(start));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("partition", partition);
        result.put("service", service);
        result.put("region", region);
        result.put("accountId", accountId);
        result.put("resourceId", Collections.unmodifiableList(resourceId));
        return Collections.unmodifiableMap(result);
    }

    /**
     * {@code aws.isVirtualHostableS3Bucket(value, allowSubDomains)}: whether an S3 bucket name can be used as a
     * virtual-hosted-style host label (3 to 63 lower-case letters, digits and hyphens starting and ending with a letter
     * or digit; with {@code allowDots} also dots, without adjacent {@code .}/{@code -} pairs, and not an IP address).
     */
    public static boolean isVirtualHostableS3Bucket(String bucket, boolean allowDots) {
        int length = bucket.length();
        if (length < 3 || length > 63) {
            return false;
        }
        if (!isLowerAlphanumeric(bucket.charAt(0)) || !isLowerAlphanumeric(bucket.charAt(length - 1))) {
            return false;
        }
        if (!allowDots) {
            for (int i = 1; i < length - 1; i++) {
                char c = bucket.charAt(i);
                if (!isLowerAlphanumeric(c) && c != '-') {
                    return false;
                }
            }
            return true;
        }
        char last = bucket.charAt(0);
        for (int i = 1; i < length; i++) {
            char c = bucket.charAt(i);
            if (c == '.') {
                if (last == '.' || last == '-') {
                    return false;
                }
            } else if (c == '-') {
                if (last == '.') {
                    return false;
                }
            } else if (!isLowerAlphanumeric(c)) {
                return false;
            }
            last = c;
        }
        return !isIpAddress(bucket);
    }

    private static boolean isLowerAlphanumeric(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }
}
