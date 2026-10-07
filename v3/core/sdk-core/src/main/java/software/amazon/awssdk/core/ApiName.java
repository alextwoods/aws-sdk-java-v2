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

package software.amazon.awssdk.core;

import java.util.Objects;

/**
 * The name and version of a library built on the SDK that contributes to a call, as in V2 (Requirement 21.6). See
 * {@link RequestOverrideConfiguration.Builder#addApiName(ApiName)}: the call's user agent carries every API name its
 * override configuration adds, and no other.
 *
 * <p>An API name named {@value #SDK_METRICS} contributes its version as a feature identifier to the user agent's
 * {@code m/} feature list (V2 business metrics); any other is appended as {@code name/version}.
 */
public final class ApiName {
    /** The API name whose versions are user-agent feature identifiers (V2 {@code ApplyUserAgentStage.SDK_METRICS}). */
    public static final String SDK_METRICS = "sdk-metrics";

    private final String name;
    private final String version;

    private ApiName(BuilderImpl b) {
        this.name = Objects.requireNonNull(b.name, "name must not be null");
        this.version = Objects.requireNonNull(b.version, "version must not be null");
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiName that = (ApiName) o;
        return name.equals(that.name) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + version.hashCode();
    }

    @Override
    public String toString() {
        return "ApiName(name=" + name + ", version=" + version + ")";
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /** Builder of an {@link ApiName}; both values are required. */
    public interface Builder {
        /** Sets the name of the API. */
        Builder name(String name);

        /** Sets the version of the API. */
        Builder version(String version);

        /**
         * Builds the API name.
         *
         * @throws NullPointerException if the name or version is not set
         */
        ApiName build();
    }

    private static final class BuilderImpl implements Builder {
        private String name;
        private String version;

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        @Override
        public ApiName build() {
            return new ApiName(this);
        }
    }
}
