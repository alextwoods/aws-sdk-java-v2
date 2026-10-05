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

package software.amazon.awssdk.tools.servicefleet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * One service of the V2 Launch Parity Baseline: a C2J model under a V2 {@code services/<module>} module's
 * {@code src/main/resources/codegen-resources} (the root, or one sub-directory per service of modules that bundle
 * several, such as {@code dynamodb} with DynamoDB Streams and {@code waf} with WAF Regional).
 *
 * <p>Only the V2 identity of the service is read: its {@code metadata.serviceId}, from which V2 derives the service
 * name, and the {@code customization.config} settings that change which client types V2 generates. The C2J model is
 * never a generation input.
 *
 * @param module the V2 module directory name (also the V3 artifact ID)
 * @param subPackage the V2 {@code packageName} customization of a bundled service (for example {@code streams}), or
 *        {@code null} for the module's own service
 * @param serviceId the C2J {@code metadata.serviceId}, or {@code null} when the module has no C2J model
 * @param syncClient whether V2 generates a sync client ({@code false} for {@code skipSyncClientGeneration})
 * @param sharesModel whether V2 generates the model types into another service's package ({@code shareModelWith})
 * @param baseExceptionName the V2 {@code sdkModeledExceptionBaseClassName} customization, or {@code null}
 */
public record V2Service(String module, String subPackage, String serviceId, boolean syncClient, boolean sharesModel,
                        String baseExceptionName) {

    /** Root package of every V2 and V3 service client. */
    public static final String PACKAGE_PREFIX = "software.amazon.awssdk.services.";

    private static final Pattern MODULE = Pattern.compile("<module>([^<]+)</module>");
    private static final Path CODEGEN_RESOURCES = Path.of("src/main/resources/codegen-resources");

    public V2Service {
        Objects.requireNonNull(module, "module");
    }

    /** The service's baseline name: the module, or {@code <module>.<subPackage>} for a bundled service. */
    public String name() {
        return subPackage == null ? module : module + "." + subPackage;
    }

    /** The V2 (and V3) client package. */
    public String packageName() {
        return PACKAGE_PREFIX + name();
    }

    /**
     * V2's service name ({@code DefaultNamingStrategy.getServiceName}): the PascalCase service ID without a leading
     * {@code Amazon} or {@code AWS} and a trailing {@code Service}, as in {@code DynamoDb} or {@code S3}.
     */
    public String serviceName() {
        if (serviceId == null) {
            throw new IllegalStateException(name() + " has no C2J serviceId");
        }
        StringBuilder pascal = new StringBuilder();
        for (String word : splitOnWordBoundaries(serviceId)) {
            if (!word.isEmpty()) {
                String lower = word.toLowerCase(Locale.ROOT);
                pascal.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
        }
        String name = removeLeading(removeLeading(pascal.toString(), "amazon"), "aws");
        return name.toLowerCase(Locale.ROOT).endsWith("service") ? name.substring(0, name.length() - "service".length())
                                                                 : name;
    }

    /**
     * The types V2 generates for every service and that the V3 service module must therefore generate under the same
     * fully qualified names (Requirements 7.1, 8.1): the sync client interface and builder (unless V2 skips the sync
     * client), the async client interface and builder, and the service's base exception (unless V2 shares another
     * service's model package or renames it).
     */
    public List<String> requiredTypes() {
        String prefix = packageName() + "." + serviceName();
        List<String> types = new ArrayList<>();
        if (syncClient) {
            types.add(prefix + "Client");
            types.add(prefix + "ClientBuilder");
        }
        types.add(prefix + "AsyncClient");
        types.add(prefix + "AsyncClientBuilder");
        if (!sharesModel && baseExceptionName == null) {
            types.add(packageName() + ".model." + serviceName() + "Exception");
        }
        return types.stream().sorted().toList();
    }

    /** The V2 baseline: every service of every module listed by the V2 {@code services/pom.xml}, in module order. */
    public static List<V2Service> scan(Path repositoryRoot) {
        List<V2Service> services = new ArrayList<>();
        Path servicesDirectory = repositoryRoot.resolve("services");
        Matcher matcher = MODULE.matcher(read(servicesDirectory.resolve("pom.xml")));
        while (matcher.find()) {
            String module = matcher.group(1).trim();
            Path resources = servicesDirectory.resolve(module).resolve(CODEGEN_RESOURCES);
            List<Path> directories = new ArrayList<>();
            if (Files.isRegularFile(resources.resolve("service-2.json"))) {
                directories.add(resources);
            } else if (Files.isDirectory(resources)) {
                try (Stream<Path> list = Files.list(resources)) {
                    list.filter(d -> Files.isRegularFile(d.resolve("service-2.json"))).sorted()
                        .forEach(directories::add);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            if (directories.isEmpty()) {
                services.add(new V2Service(module, null, null, true, false, null));
            }
            for (Path directory : directories) {
                services.add(of(module, directory));
            }
        }
        return services;
    }

    private static V2Service of(String module, Path directory) {
        ObjectNode metadata = Node.parse(read(directory.resolve("service-2.json"))).expectObjectNode()
                                  .expectObjectMember("metadata");
        String serviceId = metadata.getStringMember("serviceId").map(s -> s.getValue()).orElse(null);
        ObjectNode customization = Node.objectNode();
        Path config = directory.resolve("customization.config");
        if (Files.isRegularFile(config)) {
            try {
                // V2 customization files may carry // comments, which V2's Jackson reader allows.
                customization = Node.parseJsonWithComments(read(config), config.toString()).expectObjectNode();
            } catch (RuntimeException e) {
                throw new IllegalStateException("Unable to read the V2 customization " + config + ": "
                                                + e.getMessage(), e);
            }
        }
        ObjectNode share = customization.getObjectMember("shareModelConfig").orElse(Node.objectNode());
        String subPackage = share.getStringMember("packageName").map(s -> s.getValue()).orElse(null);
        return new V2Service(module, subPackage, serviceId,
                             !customization.getBooleanMemberOrDefault("skipSyncClientGeneration", false),
                             share.getStringMember("shareModelWith").isPresent(),
                             customization.getStringMember("sdkModeledExceptionBaseClassName")
                                          .map(s -> s.getValue()).orElse(null));
    }

    /** V2's {@code CodegenNamingUtils.splitOnWordBoundaries}. */
    static String[] splitOnWordBoundaries(String value) {
        String result = value.replaceAll("[^A-Za-z0-9]+", " ");
        result = result.replaceAll("([^a-z]{2,})v([0-9]+)", "$1 v$2 ").replaceAll("([^A-Z]{2,})V([0-9]+)", "$1 V$2 ");
        result = String.join(" ", result.split("(?<=[a-z])(?=[A-Z]([a-zA-Z]|[0-9]))"));
        result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        result = result.replaceAll("([0-9])([a-zA-Z])", "$1 $2");
        return result.replaceAll(" +", " ").trim().split(" ");
    }

    private static String removeLeading(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file, e);
        }
    }
}
