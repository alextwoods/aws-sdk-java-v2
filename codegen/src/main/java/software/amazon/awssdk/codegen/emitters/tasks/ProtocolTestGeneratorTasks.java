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

package software.amazon.awssdk.codegen.emitters.tasks;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.protocoltest.ProtocolTestGenerator;

/**
 * Generates the protocol tests of a service whose {@code customization.config} sets {@code "protocolTests": true}
 * (design D.1) into the tests directory. Has no tasks otherwise, so the output of every other service is unchanged.
 *
 * <p>The generator renders every class in the constructor, which runs when the composite task tree is created, so a skip
 * list error or an ungeneratable case fails before any generator task writes a file.
 */
public final class ProtocolTestGeneratorTasks extends BaseGeneratorTasks {
    private final ProtocolTestGenerator generator;

    ProtocolTestGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.generator = ProtocolTestGenerator.isEnabled(model) ? ProtocolTestGenerator.create(model) : null;
    }

    @Override
    protected boolean hasTasks() {
        return generator != null;
    }

    @Override
    protected List<GeneratorTask> createTasks() {
        List<GeneratorTask> tasks = new ArrayList<>();
        for (ClassSpec spec : generator.classSpecs()) {
            tasks.add(createPoetGeneratorTestTask(spec));
        }
        return tasks;
    }
}
