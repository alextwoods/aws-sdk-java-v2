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

package software.amazon.awssdk.awscodegen.validation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Trimmed copies of real V2 {@code codegen-resources} directories (test resources next to this class):
 * <ul>
 *   <li>{@code v2-signer}: the flat V2 layout of {@code services/signer} (C2J service and documentation models,
 *       paginators, waiters, examples, endpoint BDD and endpoint tests, customization);</li>
 *   <li>{@code v2-nested}: a nested multi-service layout with an endpoint rule set renamed to {@code rules.json}, a C2J
 *       {@code api-2.json}, a customization file containing comments ({@code services/gamelift}) and smoke tests
 *       ({@code services/route53resolver}).</li>
 * </ul>
 */
public final class V2Layouts {
    public static final String SIGNER = "v2-signer";
    public static final String NESTED = "v2-nested";

    private V2Layouts() {
    }

    /** The on-disk directory of a layout. */
    public static Path directory(String layout) {
        URL url = V2Layouts.class.getResource(layout);
        if (url == null) {
            throw new IllegalStateException("Missing test resource directory " + layout);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Copies every file of {@code layout} into {@code target} (created if needed); returns the relative paths. */
    public static List<Path> copy(String layout, Path target) {
        Path source = directory(layout);
        try (Stream<Path> walk = Files.walk(source)) {
            List<Path> files = walk.filter(Files::isRegularFile).map(source::relativize).sorted().toList();
            for (Path relative : files) {
                Path destination = target.resolve(relative.toString());
                Files.createDirectories(destination.getParent());
                Files.copy(source.resolve(relative), destination);
            }
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
