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

package software.amazon.awssdk.v2migration.recipes.runtime;

/**
 * {@code interceptors.hooks}: converts V2 interceptors (Requirement 11.12; design "Interceptor migration rules") to the
 * V3 hooks and typed contexts that the Parity Manifest's recipe replacements name. A V2 hook override becomes the V3
 * hook: its context parameter takes the V3 context type, its {@code ExecutionAttributes} parameter is removed (only if
 * unread, since attribute keys have no automatic typed-context mapping), its context accessor calls follow their own
 * replacements, and the implemented V2 interface and its imports follow their type replacements. A hook that the
 * replacement renames (for example {@code modifyHttpRequest} to {@code modifyBeforeSigning}) runs at a different
 * lifecycle point or frequency in V3, so it is converted only when its body has no side effects: it may build values
 * from the interceptor and HTTP types and the JDK's value types, but not call other code, create other objects or
 * write state, since that would now run at the new point (for example once per attempt). Every other hook, and every
 * hook whose manifest entry is a diagnostic, is left unchanged and reported.
 */
public final class InterceptorRecipe extends ReplacementRecipe {

    public InterceptorRecipe() {
        super("interceptors.hooks",
              "Converts V2 ExecutionInterceptor hooks, typed contexts and interceptor types to the V3 replacements "
              + "the Parity Manifest names.",
              Family.INTERCEPTORS);
    }
}
