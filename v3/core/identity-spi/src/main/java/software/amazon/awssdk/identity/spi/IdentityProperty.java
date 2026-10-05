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

package software.amazon.awssdk.identity.spi;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A named property of a {@link ResolveIdentityRequest}, with the V2 name and factory.
 *
 * <p>A property is identified by its namespace (the class that owns it) and name, and each namespace/name pair may be
 * created once per class loader: declare properties as shared static constants. Immutable; equal when namespace and
 * name are equal.
 *
 * @param <T> the property value type
 */
public final class IdentityProperty<T> {
    private static final ConcurrentMap<String, IdentityProperty<?>> CREATED = new ConcurrentHashMap<>();

    private final String namespace;
    private final String name;

    private IdentityProperty(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    /**
     * A new property named {@code name} in the namespace of {@code namespace}.
     *
     * @throws IllegalArgumentException if {@code name} is blank, or a property with this namespace and name exists
     */
    public static <T> IdentityProperty<T> create(Class<?> namespace, String name) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("An identity property name must not be blank.");
        }
        IdentityProperty<T> property = new IdentityProperty<>(namespace.getName(), name);
        IdentityProperty<?> previous = CREATED.putIfAbsent(namespace.getName() + '#' + name, property);
        if (previous != null) {
            throw new IllegalArgumentException("An identity property named " + name + " already exists in namespace "
                                               + namespace.getName() + "; reference it from one shared constant.");
        }
        return property;
    }

    /** Name of the class that owns this property. */
    public String namespace() {
        return namespace;
    }

    /** Name of this property within its namespace. */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IdentityProperty<?> that && namespace.equals(that.namespace) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + name.hashCode();
    }

    @Override
    public String toString() {
        return "IdentityProperty(" + namespace + "#" + name + ")";
    }
}
