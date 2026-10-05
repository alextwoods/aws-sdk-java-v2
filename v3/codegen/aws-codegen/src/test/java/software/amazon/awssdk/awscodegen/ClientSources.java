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

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The generated client sources ({@link ClientGenerator}) of a service root package, so that tests of other emitters
 * can compare their exact output without restating the client files, which {@code ClientGenerationTest} covers.
 */
final class ClientSources {
    private static final Pattern CLIENT_FILE = Pattern.compile(
        "(Default)?[A-Z]\\w*?(Async)?Client(Builder)?\\.java|[A-Z]\\w*OperationBindings\\.java");

    private ClientSources() {
    }

    /** {@code files} without the client sources generated into a service root package. */
    static List<Path> withoutClients(List<Path> files) {
        return files.stream().filter(file -> !isClientFile(file)).toList();
    }

    static boolean isClientFile(Path file) {
        Path parent = file.getParent();
        String directory = parent == null ? "" : parent.toString().replace('\\', '/');
        return !directory.contains("/model") && !directory.contains("/internal")
               && CLIENT_FILE.matcher(file.getFileName().toString()).matches();
    }
}
