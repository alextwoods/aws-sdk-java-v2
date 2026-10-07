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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.retries.api.RetryStrategy;

/** Registers decorated extension instances by name; {@link #run} returns what happened, one line per step. */
public final class RegistryApp {
    private RegistryApp() {
    }

    /** Runs the registry workflow; {@code endpoint} is unused (no service is called). */
    public static String run(String endpoint) {
        ExtensionRegistry registry = new ExtensionRegistry();
        RetryStrategy standard = named(RetryStrategy.class, "standard");
        RetryStrategy adaptive = named(RetryStrategy.class, "adaptive");
        HttpSigner<?> sigv4 = named(HttpSigner.class, "sigv4");
        List<String> transcript = new ArrayList<>();
        transcript.add("register standard: " + registry.registerRetryStrategy("standard", standard));
        transcript.add("register adaptive: " + registry.registerRetryStrategy("adaptive", adaptive));
        transcript.add("register standard again: " + registry.registerRetryStrategy("standard", adaptive));
        transcript.add("register sigv4: " + registry.registerSigner("sigv4", sigv4));
        transcript.add("standard is shared: " + (registry.retryStrategy("standard").orElse(null) == standard));
        transcript.add("sigv4 is a signer: " + registry.signer("sigv4").map(HttpSigner.class::isInstance).orElse(false));
        transcript.add("missing: " + registry.retryStrategy("legacy").isPresent());
        transcript.add("names: " + registry.names());
        transcript.add("describe standard: " + registry.retryStrategy("standard").map(Object::toString).orElse("-"));
        return String.join("\n", transcript);
    }

    /** A placeholder instance of the extension interface {@code type} that only describes itself. */
    private static <T> T named(Class<T> type, String name) {
        Object proxy = Proxy.newProxyInstance(RegistryApp.class.getClassLoader(), new Class<?>[] {type},
                                              (self, method, args) -> {
                                                  switch (method.getName()) {
                                                      case "toString":
                                                          return type.getSimpleName() + "(" + name + ")";
                                                      case "hashCode":
                                                          return System.identityHashCode(self);
                                                      case "equals":
                                                          return self == args[0];
                                                      default:
                                                          throw new UnsupportedOperationException(method.getName());
                                                  }
                                              });
        return type.cast(proxy);
    }
}
