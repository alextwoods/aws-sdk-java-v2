package software.amazon.awssdk.services.registry.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.serde.TypeRegistry;

/**
 * The type registry of the registry#RegistryService service's model package: the structures the service lists in its
 * {@code shapes} property, by shape ID. Use it to turn a document with a discriminator into a model
 * object with {@code TypeRegistry.deserialize}, or to compose it with other registries.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class RegistryTypeRegistry {
    /**
     * The registry. Every access returns the same instance; creating it loads no model class.
     */
    public static final TypeRegistry INSTANCE = TypeRegistry.empty();

    private RegistryTypeRegistry() {
    }
}
