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

package software.amazon.awssdk.v2migration;

import java.util.List;
import java.util.Map;

/**
 * The migration concern of an item: the coverage areas of Requirement 17.6 and the disposition areas of Requirement
 * 17.11, so the report can show that every encountered dependency, configuration option, interceptor hook, HTTP option,
 * retry, signer, streaming and high-level-library item received a disposition. Derived deterministically from the item
 * kind and the V2 package of the item's entry ID by {@link #of(ItemKind, String)}.
 */
public enum MigrationCategory {
    DEPENDENCY,
    BOM,
    IMPORT,
    BUILDER,
    CLIENT,
    CONFIGURATION,
    INTERCEPTOR,
    HTTP,
    RETRY,
    SIGNER,
    STREAMING,
    HIGH_LEVEL_LIBRARY,
    /** Any other V2 public API, and source the compiler could not attribute. */
    API;

    private static final String V2 = "software.amazon.awssdk.";

    /** Package (or type) prefixes in precedence order: the first matching prefix decides. */
    private static final List<Map.Entry<String, MigrationCategory>> PREFIXES = List.of(
        Map.entry(V2 + "enhanced.", HIGH_LEVEL_LIBRARY),
        Map.entry(V2 + "transfer.s3.", HIGH_LEVEL_LIBRARY),
        Map.entry(V2 + "policybuilder.iam.", HIGH_LEVEL_LIBRARY),
        Map.entry(V2 + "eventnotifications.", HIGH_LEVEL_LIBRARY),
        Map.entry(V2 + "messagemanager.sns.", HIGH_LEVEL_LIBRARY),
        Map.entry(V2 + "core.interceptor.", INTERCEPTOR),
        Map.entry(V2 + "awscore.interceptor.", INTERCEPTOR),
        Map.entry(V2 + "core.retry.", RETRY),
        Map.entry(V2 + "awscore.retry.", RETRY),
        Map.entry(V2 + "retries.", RETRY),
        Map.entry(V2 + "auth.signer.", SIGNER),
        Map.entry(V2 + "core.signer.", SIGNER),
        Map.entry(V2 + "http.auth.", SIGNER),
        Map.entry(V2 + "awscore.presigner.", SIGNER),
        Map.entry(V2 + "http.", HTTP),
        Map.entry(V2 + "core.async.", STREAMING),
        Map.entry(V2 + "core.sync.", STREAMING),
        Map.entry(V2 + "core.ResponseInputStream", STREAMING),
        Map.entry(V2 + "core.ResponseBytes", STREAMING),
        Map.entry(V2 + "core.FileTransformerConfiguration", STREAMING),
        Map.entry(V2 + "core.client.builder.", BUILDER),
        Map.entry(V2 + "awscore.client.builder.", BUILDER),
        Map.entry(V2 + "core.client.config.", CONFIGURATION),
        Map.entry(V2 + "awscore.client.config.", CONFIGURATION),
        Map.entry(V2 + "awscore.defaultsmode.", CONFIGURATION),
        Map.entry(V2 + "regions.", CONFIGURATION),
        Map.entry(V2 + "auth.credentials.", CONFIGURATION),
        Map.entry(V2 + "identity.spi.", CONFIGURATION),
        Map.entry(V2 + "profiles.", CONFIGURATION));

    /** The category of an item of {@code kind} whose entry ID (or candidate ID) is {@code id}. */
    public static MigrationCategory of(ItemKind kind, String id) {
        switch (kind) {
            case DEPENDENCY:
            case MANAGED_DEPENDENCY:
            case EFFECTIVE_DEPENDENCY:
                return DEPENDENCY;
            case BOM_IMPORT:
                return BOM;
            case IMPORT:
            case STATIC_IMPORT:
                return IMPORT;
            case CONFIGURATION_KEY:
                return CONFIGURATION;
            case UNATTRIBUTED_SOURCE:
                return API;
            default:
                break;
        }
        if (id == null || !id.startsWith("api:")) {
            return API;
        }
        String member = id.substring("api:".length());
        int hash = member.indexOf('#');
        String owner = hash < 0 ? member : member.substring(0, hash);
        for (Map.Entry<String, MigrationCategory> prefix : PREFIXES) {
            if (owner.startsWith(prefix.getKey())) {
                return prefix.getValue();
            }
        }
        String simpleName = owner.substring(owner.lastIndexOf('.') + 1);
        if (simpleName.endsWith("Builder") || simpleName.contains("$Builder")) {
            return BUILDER;
        }
        if (owner.startsWith(V2 + "services.") && !simpleName.contains("$")
            && simpleName.endsWith("Client")) {
            return CLIENT;
        }
        return API;
    }
}
