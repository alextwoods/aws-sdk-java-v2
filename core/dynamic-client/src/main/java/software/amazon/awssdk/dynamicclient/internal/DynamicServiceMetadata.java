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

package software.amazon.awssdk.dynamicclient.internal;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The service a dynamic client calls, resolved from its Smithy model, and the names the client configuration derives
 * from it.
 *
 * <p>The names follow codegen ({@code AddSmithyMetadata} and {@code DefaultSmithyNamingStrategy}), so a dynamic client
 * resolves the same defaults as the generated client of the same service: the {@code aws.api#service} trait gives the
 * SDK ID and the endpoint prefix, and the {@code aws.auth#sigv4} trait the signing name. A service without the
 * {@code aws.api#service} trait, which codegen rejects, uses its shape name as SDK ID and its lower-cased shape name as
 * endpoint prefix.
 */
@SdkInternalApi
public final class DynamicServiceMetadata {
    private final ServiceShape service;
    private final String sdkId;
    private final String serviceName;
    private final String endpointPrefix;
    private final String signingName;

    private DynamicServiceMetadata(ServiceShape service) {
        this.service = service;
        Optional<ServiceTrait> serviceTrait = service.getTrait(ServiceTrait.class);
        this.sdkId = serviceTrait.map(ServiceTrait::getSdkId).orElseGet(() -> service.getId().getName());
        this.serviceName = removeRedundantPrefixesAndSuffixes(pascalCase(sdkId));
        this.endpointPrefix = serviceTrait.map(ServiceTrait::getEndpointPrefix)
                                          .orElseGet(() -> StringUtils.lowerCase(service.getId().getName()));
        this.signingName = service.getTrait(SigV4Trait.class)
                                  .map(SigV4Trait::getName)
                                  .orElseGet(() -> serviceTrait.map(ServiceTrait::getArnNamespace).orElse(""));
    }

    /**
     * Resolves the service of a model.
     *
     * <ul>
     *     <li>With a service ID, the model must contain a service shape with that ID, whatever the number of services
     *     in the model (Requirements 26.8, 26.9).</li>
     *     <li>Without one, the model must contain exactly one service shape (Requirements 26.2 to 26.4).</li>
     * </ul>
     *
     * @param model the model, not null
     * @param serviceId the configured service ID, or null to use the model's only service
     * @return the metadata of the resolved service
     * @throws NullPointerException if no service ID is configured and the model contains no service or more than one;
     *     the message lists the services of the model
     * @throws IllegalArgumentException if the model has no shape with the configured ID, or that shape is not a service;
     *     the message names the ID and lists the services of the model
     */
    public static DynamicServiceMetadata resolve(Model model, ShapeId serviceId) {
        Validate.paramNotNull(model, "model");
        Set<ServiceShape> services = model.getServiceShapes();
        if (serviceId != null) {
            Optional<Shape> shape = model.getShape(serviceId);
            if (!shape.isPresent()) {
                throw new IllegalArgumentException("Service " + serviceId + " is not in the model. "
                                                   + describeServices(services));
            }
            if (!shape.get().isServiceShape()) {
                throw new IllegalArgumentException("Shape " + serviceId + " is a " + shape.get().getType()
                                                   + ", not a service. " + describeServices(services));
            }
            return new DynamicServiceMetadata(shape.get().asServiceShape().get());
        }
        if (services.size() == 1) {
            return new DynamicServiceMetadata(services.iterator().next());
        }
        if (services.isEmpty()) {
            throw new NullPointerException("No serviceId is set, and the model contains no services");
        }
        throw new NullPointerException("No serviceId is set, and the model contains multiple services: "
                                       + serviceIds(services) + ". Set serviceId to one of them.");
    }

    /**
     * Returns the resolved service shape.
     *
     * @return the service shape
     */
    public ServiceShape service() {
        return service;
    }

    /**
     * Returns the SDK ID: the {@code sdkId} of the {@code aws.api#service} trait, or the service's shape name.
     *
     * @return the SDK ID, for example {@code SQS}
     */
    public String sdkId() {
        return sdkId;
    }

    /**
     * Returns the service name of the client configuration ({@code SdkClientOption.SERVICE_NAME}): the SDK ID in
     * PascalCase without a leading {@code Amazon} or {@code Aws} and without a trailing {@code Service}, as the
     * generated client's {@code serviceName()} builder hook.
     *
     * @return the service name, for example {@code Sqs}
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * Returns the endpoint prefix: the {@code endpointPrefix} of the {@code aws.api#service} trait, or the lower-cased
     * shape name of the service.
     *
     * @return the endpoint prefix, for example {@code sqs}
     */
    public String endpointPrefix() {
        return endpointPrefix;
    }

    /**
     * Returns the signing name: the {@code name} of the {@code aws.auth#sigv4} trait, else the {@code arnNamespace} of
     * the {@code aws.api#service} trait, else an empty string, as codegen.
     *
     * @return the signing name, for example {@code sqs}
     */
    public String signingName() {
        return signingName;
    }

    /**
     * Returns the SDK ID in the form of environment variable names: upper case, with spaces replaced by underscores.
     *
     * @return the SDK ID for environment variables, for example {@code SECRETS_MANAGER}
     */
    public String serviceNameForEnvironmentVariables() {
        return StringUtils.upperCase(sdkId.replace(' ', '_'));
    }

    /**
     * Returns the SDK ID in the form of profile file properties: lower case, with spaces replaced by underscores.
     *
     * @return the SDK ID for profile files, for example {@code secrets_manager}
     */
    public String serviceNameForProfileFile() {
        return StringUtils.lowerCase(serviceNameForEnvironmentVariables());
    }

    private static String describeServices(Set<ServiceShape> services) {
        if (services.isEmpty()) {
            return "The model contains no services.";
        }
        return "The model contains the services " + serviceIds(services) + ".";
    }

    private static Set<String> serviceIds(Set<ServiceShape> services) {
        Set<String> ids = new TreeSet<>();
        for (ServiceShape service : services) {
            ids.add(service.getId().toString());
        }
        return ids;
    }

    /**
     * Codegen's {@code CodegenNamingUtils.pascalCase(String)}, which is internal to the {@code utils} module: splits
     * the value on word boundaries and capitalizes each lower-cased word.
     */
    private static String pascalCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : splitOnWordBoundaries(value)) {
            result.append(StringUtils.capitalize(StringUtils.lowerCase(word)));
        }
        return result.toString();
    }

    /**
     * Codegen's {@code CodegenNamingUtils.splitOnWordBoundaries(String)}.
     */
    private static String[] splitOnWordBoundaries(String toSplit) {
        String result = toSplit.replaceAll("[^A-Za-z0-9]+", " ");
        result = result.replaceAll("([^a-z]{2,})v([0-9]+)", "$1 v$2 ")
                       .replaceAll("([^A-Z]{2,})V([0-9]+)", "$1 V$2 ");
        result = String.join(" ", result.split("(?<=[a-z])(?=[A-Z]([a-zA-Z]|[0-9]))"));
        result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        result = result.replaceAll("([0-9])([a-zA-Z])", "$1 $2");
        result = result.replaceAll(" +", " ").trim();
        return result.split(" ");
    }

    private static String removeRedundantPrefixesAndSuffixes(String name) {
        String result = removeLeading(name, "amazon");
        result = removeLeading(result, "aws");
        return removeTrailing(result, "service");
    }

    private static String removeLeading(String value, String prefix) {
        return StringUtils.lowerCase(value).startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String removeTrailing(String value, String suffix) {
        return StringUtils.lowerCase(value).endsWith(suffix) ? value.substring(0, value.length() - suffix.length())
                                                             : value;
    }
}
