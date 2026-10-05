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

package software.amazon.awssdk.awscodegen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Identity of the generator and of the JDK running it, recorded in every generation manifest.
 *
 * <p>The generator identity comes from {@code generator.properties}, which Maven filters when {@code aws-codegen} is
 * built: its coordinates and the Smithy version it is built and tested against. The JDK identity is the Java SE
 * specification version and the JDK vendor; patch-level versions are deliberately not recorded (like Maven's
 * {@code Build-Jdk-Spec}), so that equivalent builds of one JDK release produce identical artifacts (Requirement 3.6).
 *
 * @param groupId Maven group ID of {@code aws-codegen}
 * @param artifactId Maven artifact ID of {@code aws-codegen}
 * @param version version of {@code aws-codegen}
 * @param smithyVersion version of the Smithy libraries {@code aws-codegen} was built against
 * @param jdkSpecificationVersion {@code java.specification.version} of the running JDK, for example {@code 21}
 * @param jdkVendor {@code java.vendor} of the running JDK
 */
record GeneratorIdentity(String groupId, String artifactId, String version, String smithyVersion,
                         String jdkSpecificationVersion, String jdkVendor) {

    static final String RESOURCE = "generator.properties";

    /** The identity of this generator running on this JDK. */
    static GeneratorIdentity current() {
        Properties properties = new Properties();
        try (InputStream in = GeneratorIdentity.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new AwsCodegenException("aws-codegen resource " + RESOURCE + " is missing; aws-codegen must be "
                                              + "built with Maven so that its generator identity is recorded.");
            }
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AwsCodegenException("Unable to read aws-codegen resource " + RESOURCE, e);
        }
        return new GeneratorIdentity(require(properties, "groupId"), require(properties, "artifactId"),
                                     require(properties, "version"), require(properties, "smithyVersion"),
                                     System.getProperty("java.specification.version"),
                                     System.getProperty("java.vendor"));
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank() || value.contains("${")) {
            throw new AwsCodegenException("aws-codegen resource " + RESOURCE + " has no resolved '" + key + "'.");
        }
        return value;
    }
}
