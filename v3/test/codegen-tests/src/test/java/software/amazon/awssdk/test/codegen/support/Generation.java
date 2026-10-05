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

package software.amazon.awssdk.test.codegen.support;

import java.nio.file.Path;
import java.util.List;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenIntegration;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;

/**
 * One generation of the fixture service into a work directory: the model directory {@code <work>/model} and the
 * generated source and resource roots {@code <work>/out/src} and {@code <work>/out/resources}.
 *
 * @param work the work directory
 * @param result the generation result
 */
public record Generation(Path work, AwsCodegenResult result) {

    /** Generates {@code fixture}, written in {@code fileOrder}, with {@code integrations} in the given order. */
    public static Generation run(Path work, SmithyModelFixture fixture, List<String> fileOrder,
                                 List<? extends AwsCodegenIntegration> integrations) {
        return new Generation(work, AwsCodegen.generate(request(work, fixture, fileOrder), integrations));
    }

    /** The request for {@code fixture} in {@code work}, after writing the model files in {@code fileOrder}. */
    public static AwsCodegenRequest request(Path work, SmithyModelFixture fixture, List<String> fileOrder) {
        Path models = fixture.write(work.resolve("model"), fileOrder);
        return new AwsCodegenRequest(SmithyModelFixture.SERVICE, SmithyModelFixture.PACKAGE, models,
                                     sourceRoot(work)).withResourceOutputDirectory(resourceRoot(work));
    }

    public static Path sourceRoot(Path work) {
        return work.resolve("out/src");
    }

    public static Path resourceRoot(Path work) {
        return work.resolve("out/resources");
    }

    /** The published source tree. */
    public GeneratedTree sources() {
        return GeneratedTree.read(result.sourceRoot());
    }

    /** The published resource tree. */
    public GeneratedTree resources() {
        return GeneratedTree.read(result.resourceRoot());
    }
}
