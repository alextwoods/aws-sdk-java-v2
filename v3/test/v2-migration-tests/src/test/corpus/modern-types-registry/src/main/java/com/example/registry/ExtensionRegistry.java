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

package com.example.registry;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.retries.api.RetryStrategy;

/** Named, shared instances of SDK extension points (retry strategies and signers) for an application's clients. */
public final class ExtensionRegistry {
    private final Map<String, RetryStrategy> retryStrategies = new TreeMap<>();
    private final Map<String, HttpSigner<?>> signers = new TreeMap<>();

    /** Registers {@code strategy} under {@code name}; returns false if the name is taken. */
    public boolean registerRetryStrategy(String name, RetryStrategy strategy) {
        return retryStrategies.putIfAbsent(name, strategy) == null;
    }

    public Optional<RetryStrategy> retryStrategy(String name) {
        return Optional.ofNullable(retryStrategies.get(name));
    }

    /** Registers {@code signer} under {@code name}; returns false if the name is taken. */
    public boolean registerSigner(String name, HttpSigner<?> signer) {
        return signers.putIfAbsent(name, signer) == null;
    }

    public Optional<HttpSigner<?>> signer(String name) {
        return Optional.ofNullable(signers.get(name));
    }

    /** Every registered name, sorted. */
    public Set<String> names() {
        Set<String> names = new TreeSet<>(retryStrategies.keySet());
        names.addAll(signers.keySet());
        return names;
    }
}
