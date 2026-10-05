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

package software.amazon.awssdk.tools.paritymanifest;

import java.util.List;
import java.util.Locale;

/**
 * The launch-surface category of a Parity Manifest entry, used by {@link LaunchGate} to compare the candidate with
 * the manifest one capability area at a time (Requirements 16.4, 16.5, 16.9, 16.10). Categories only group entries
 * for reporting: every non-deprecated entry is gated by the same rules whatever its category, except
 * {@link #MODULE_PATH} behaviors, which are verified structurally (see {@link LaunchGate}).
 *
 * <p>Categorization is deterministic and derived from the entry ID alone:
 * <ul>
 *   <li>{@code service:} entries are {@link #SERVICE}, {@code http-mode:} entries {@link #HTTP_MODE} and
 *       {@code artifact:} entries {@link #ARTIFACT};</li>
 *   <li>{@code behavior:} entries by the first segment of their dotted ID (for example {@code checksum.*} is
 *       {@link #CHECKSUM}, {@code module-path.*} is {@link #MODULE_PATH});</li>
 *   <li>{@code config:} entries by keywords of their key (region, profile, credentials, checksum, compression,
 *       telemetry, endpoint, logging), otherwise {@link #CONFIGURATION};</li>
 *   <li>{@code api:} entries by the package and simple name of the declaring top-level type: in a generated service
 *       package {@code software.amazon.awssdk.services.<service>} the {@code paginators}, {@code waiters},
 *       {@code presigner}, {@code endpoints} and {@code model} sub-packages, {@code *Client}/{@code *ClientBuilder}
 *       and {@code *Utilities} types, and everything else as a {@link #CUSTOMIZATION}; outside service packages by
 *       runtime package.</li>
 * </ul>
 */
public enum LaunchCategory {
    /** A V2 AWS service client module (16.4). */
    SERVICE,
    /** Generated sync/async clients and builders (16.5). */
    CLIENT,
    /** Generated model types (16.5). */
    MODEL,
    /** Modeled exceptions (16.5). */
    EXCEPTION,
    /** Streaming operations and bodies (16.5). */
    STREAMING,
    /** Event Stream operations (16.5). */
    EVENT_STREAM,
    /** Endpoint providers and endpoint resolution (16.5, 16.10). */
    ENDPOINT_PROVIDER,
    /** Hand-written service customizations (16.5). */
    CUSTOMIZATION,
    /** Paginators (16.5). */
    PAGINATOR,
    /** Waiters (16.5). */
    WAITER,
    /** Presigners (16.5). */
    PRESIGNER,
    /** Service utilities (16.5). */
    UTILITY,
    /** HTTP Implementations and their modes (16.9). */
    HTTP_MODE,
    /** Configuration settings and service defaults (16.10). */
    CONFIGURATION,
    /** Metrics, user agent and tracing (16.10). */
    TELEMETRY,
    /** Logging (16.10). */
    LOGGING,
    /** Shared profile files (16.10). */
    PROFILE,
    /** Region resolution (16.10). */
    REGION,
    /** Credentials and identity (16.10). */
    CREDENTIALS,
    /** Checksums (16.10). */
    CHECKSUM,
    /** Request compression (16.10). */
    COMPRESSION,
    /** Module-path use (16.10). */
    MODULE_PATH,
    /** GraalVM native-image use (16.10). */
    NATIVE_IMAGE,
    /** Published artifacts (16.2). */
    ARTIFACT,
    /** Anything else (retries, signing, interceptors, high-level libraries, other runtime APIs). */
    OTHER;

    private static final String SERVICES_PACKAGE = "software.amazon.awssdk.services.";

    /** Runtime package prefixes outside service packages, most specific first. */
    private static final List<PackageRule> RUNTIME_PACKAGES = List.of(
        new PackageRule("software.amazon.awssdk.core.pagination.", PAGINATOR),
        new PackageRule("software.amazon.awssdk.core.waiters.", WAITER),
        new PackageRule("software.amazon.awssdk.awscore.presigner.", PRESIGNER),
        new PackageRule("software.amazon.awssdk.awscore.eventstream.", EVENT_STREAM),
        new PackageRule("software.amazon.awssdk.core.eventstream.", EVENT_STREAM),
        new PackageRule("software.amazon.awssdk.eventstream.", EVENT_STREAM),
        new PackageRule("software.amazon.awssdk.core.sync.", STREAMING),
        new PackageRule("software.amazon.awssdk.core.async.", STREAMING),
        new PackageRule("software.amazon.awssdk.core.checksums.", CHECKSUM),
        new PackageRule("software.amazon.awssdk.checksums.", CHECKSUM),
        new PackageRule("software.amazon.awssdk.core.CompressionConfiguration", COMPRESSION),
        new PackageRule("software.amazon.awssdk.awscore.endpoint.", ENDPOINT_PROVIDER),
        new PackageRule("software.amazon.awssdk.endpoints.", ENDPOINT_PROVIDER),
        new PackageRule("software.amazon.awssdk.regions.", REGION),
        new PackageRule("software.amazon.awssdk.profiles.", PROFILE),
        new PackageRule("software.amazon.awssdk.auth.credentials.", CREDENTIALS),
        new PackageRule("software.amazon.awssdk.auth.token.", CREDENTIALS),
        new PackageRule("software.amazon.awssdk.identity.", CREDENTIALS),
        new PackageRule("software.amazon.awssdk.metrics.", TELEMETRY),
        new PackageRule("software.amazon.awssdk.utils.Logger", LOGGING),
        new PackageRule("software.amazon.awssdk.http.apache.", HTTP_MODE),
        new PackageRule("software.amazon.awssdk.http.apache5.", HTTP_MODE),
        new PackageRule("software.amazon.awssdk.http.urlconnection.", HTTP_MODE),
        new PackageRule("software.amazon.awssdk.http.nio.netty.", HTTP_MODE),
        new PackageRule("software.amazon.awssdk.http.crt.", HTTP_MODE));

    /** Configuration-key keywords, checked in order against the lower-cased key. */
    private static final List<KeywordRule> CONFIG_KEYWORDS = List.of(
        new KeywordRule(CHECKSUM, "checksum"),
        new KeywordRule(COMPRESSION, "compression"),
        new KeywordRule(TELEMETRY, "metric", "ua_app_id", "uaappid", "trace"),
        new KeywordRule(LOGGING, "log"),
        new KeywordRule(ENDPOINT_PROVIDER, "endpoint", "fips", "dualstack", "dual_stack"),
        new KeywordRule(CREDENTIALS, "access_key", "accesskey", "secret", "session_token", "sessiontoken",
                        "credential", "role", "web_identity", "webidentity", "sso_", "sso.", "container_",
                        "container.", "ec2_metadata", "ec2metadata", "token"),
        new KeywordRule(PROFILE, "profile", "config_file", "configfile", "shared_credentials"),
        new KeywordRule(REGION, "region"));

    private record PackageRule(String prefix, LaunchCategory category) {
    }

    private record KeywordRule(LaunchCategory category, String... keywords) {
        boolean matches(String key) {
            for (String keyword : keywords) {
                if (key.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** The category of {@code entry}. */
    public static LaunchCategory of(ManifestEntry entry) {
        return of(entry.kind(), entry.id());
    }

    /** The category of an entry of {@code kind} with {@code id}. */
    public static LaunchCategory of(EntryKind kind, String id) {
        return switch (kind) {
            case SERVICE -> SERVICE;
            case HTTP_MODE -> HTTP_MODE;
            case ARTIFACT -> ARTIFACT;
            case BEHAVIOR -> behavior(id.substring("behavior:".length()));
            case CONFIGURATION -> configuration(id.substring(id.indexOf(':', "config:".length()) + 1));
            case PUBLIC_TYPE, PUBLIC_MEMBER -> api(typeName(id));
        };
    }

    /** The binary name of the type an {@code api:} ID names or declares. */
    static String typeName(String apiId) {
        String rest = apiId.substring("api:".length());
        int hash = rest.indexOf('#');
        return hash < 0 ? rest : rest.substring(0, hash);
    }

    private static LaunchCategory behavior(String dotted) {
        int dot = dotted.indexOf('.');
        String head = dot < 0 ? dotted : dotted.substring(0, dot);
        return switch (head) {
            case "telemetry" -> TELEMETRY;
            case "logging" -> LOGGING;
            case "profile" -> PROFILE;
            case "region" -> REGION;
            case "credentials" -> CREDENTIALS;
            case "checksum" -> CHECKSUM;
            case "compression" -> COMPRESSION;
            case "module-path" -> MODULE_PATH;
            case "native-image" -> NATIVE_IMAGE;
            case "endpoint" -> ENDPOINT_PROVIDER;
            case "paginators" -> PAGINATOR;
            case "waiters" -> WAITER;
            case "presigning" -> PRESIGNER;
            case "event-streams" -> EVENT_STREAM;
            case "streaming" -> STREAMING;
            case "http" -> HTTP_MODE;
            case "defaults-mode", "service-defaults" -> CONFIGURATION;
            default -> OTHER;
        };
    }

    private static LaunchCategory configuration(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (KeywordRule rule : CONFIG_KEYWORDS) {
            if (rule.matches(lower)) {
                return rule.category();
            }
        }
        return CONFIGURATION;
    }

    private static LaunchCategory api(String binaryName) {
        if (binaryName.startsWith(SERVICES_PACKAGE)) {
            return serviceApi(binaryName.substring(SERVICES_PACKAGE.length()));
        }
        for (PackageRule rule : RUNTIME_PACKAGES) {
            if (binaryName.startsWith(rule.prefix())) {
                return rule.category();
            }
        }
        return OTHER;
    }

    /** {@code relative} is the binary name after {@code software.amazon.awssdk.services.}. */
    private static LaunchCategory serviceApi(String relative) {
        String[] segments = relative.split("\\.");
        if (segments.length < 2) {
            return CUSTOMIZATION;
        }
        String topLevel = segments[segments.length - 1];
        int dollar = topLevel.indexOf('$');
        String simple = dollar < 0 ? topLevel : topLevel.substring(0, dollar);
        if (segments.length > 2) {
            switch (segments[1]) {
                case "paginators" -> {
                    return PAGINATOR;
                }
                case "waiters" -> {
                    return WAITER;
                }
                case "presigner" -> {
                    return PRESIGNER;
                }
                case "endpoints" -> {
                    return ENDPOINT_PROVIDER;
                }
                case "model" -> {
                    return model(simple);
                }
                default -> {
                    return CUSTOMIZATION;
                }
            }
        }
        if (simple.endsWith("Client") || simple.endsWith("ClientBuilder")) {
            return CLIENT;
        }
        if (simple.endsWith("Utilities")) {
            return UTILITY;
        }
        return CUSTOMIZATION;
    }

    private static LaunchCategory model(String simple) {
        if (simple.endsWith("Exception")) {
            return EXCEPTION;
        }
        if (simple.contains("EventStream") || simple.endsWith("ResponseHandler")) {
            return EVENT_STREAM;
        }
        return MODEL;
    }
}
