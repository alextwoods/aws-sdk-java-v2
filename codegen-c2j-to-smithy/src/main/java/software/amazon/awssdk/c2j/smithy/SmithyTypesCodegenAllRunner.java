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

package software.amazon.awssdk.c2j.smithy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Build-time entry point: for each benchmark C2J service directory under {@code <resources>}, run
 * the C2J→Smithy conversion + smithy-java type codegen ({@link SmithyTypesCodegenRunner}) into a
 * shared output dir, each service in its own {@code com.amazonaws.<service>} package.
 *
 * <p>Args: {@code <codegen-resources-dir> <output-dir>}.
 */
public final class SmithyTypesCodegenAllRunner {

    // C2J service dir -> generated Java package. Packages are distinct from v2's
    // software.amazon.awssdk.services.* so the native types are purely additive.
    private static final Map<String, String> SERVICES = new LinkedHashMap<>();

    static {
        SERVICES.put("json-rpc-1-0", "com.amazonaws.jsonrpc10dataplane");
        SERVICES.put("rest-json", "com.amazonaws.restjsondataplane");
        SERVICES.put("rest-xml", "com.amazonaws.restxmldataplane");
        SERVICES.put("rpc-v2-cbor", "com.amazonaws.rpcv2cbordataplane");
        SERVICES.put("query", "com.amazonaws.querydataplane");
    }

    private SmithyTypesCodegenAllRunner() {
    }

    public static void main(String[] args) {
        Path resources = Path.of(args[0]);
        Path outputDir = Path.of(args[1]);
        for (Map.Entry<String, String> e : SERVICES.entrySet()) {
            Path serviceJson = resources.resolve(e.getKey()).resolve("service-2.json");
            if (!Files.exists(serviceJson)) {
                System.out.println("[smithy-types] skip (no service-2.json): " + serviceJson);
                continue;
            }
            String nativeNamespace = e.getValue();
            try {
                SmithyTypesCodegenRunner.generate(serviceJson, outputDir, nativeNamespace);
                System.out.println("[smithy-types] generated " + nativeNamespace + " from " + e.getKey());
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Failed to generate Smithy types for " + e.getKey(), ex);
            }
        }
    }

    // Keep a checked-exception-free helper signature usable elsewhere.
    static void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
