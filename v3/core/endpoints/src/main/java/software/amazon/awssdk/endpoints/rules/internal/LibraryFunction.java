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

package software.amazon.awssdk.endpoints.rules.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.endpoints.internal.EndpointValues;
import software.amazon.awssdk.endpoints.partition.PartitionMetadata;
import software.amazon.awssdk.endpoints.partition.Partitions;

/**
 * The library functions of the endpoint rules language that take evaluated arguments: the Smithy standard library
 * ({@code substring}, {@code uriEncode}, {@code isValidHostLabel}, {@code parseURL}, {@code split}) and the AWS
 * extension ({@code aws.partition}, {@code aws.parseArn}, {@code aws.isVirtualHostableS3Bucket}). Each has the exact
 * semantics of the canonical Smithy rules evaluator; see {@link RulesFunctions} for the algorithms.
 */
public enum LibraryFunction {
    SUBSTRING("substring", 4) {
        @Override
        Object invoke(Object[] a) {
            return RulesFunctions.substring(string(a[0]), integer(a[1]), integer(a[2]), bool(a[3]));
        }
    },
    URI_ENCODE("uriEncode", 1) {
        @Override
        Object invoke(Object[] a) {
            return RulesFunctions.uriEncode(string(a[0]));
        }
    },
    IS_VALID_HOST_LABEL("isValidHostLabel", 2) {
        @Override
        Object invoke(Object[] a) {
            return RulesFunctions.isValidHostLabel(string(a[0]), bool(a[1]));
        }
    },
    PARSE_URL("parseURL", 1) {
        @Override
        Object invoke(Object[] a) {
            return RulesFunctions.parseUrl(string(a[0]));
        }
    },
    SPLIT("split", 3) {
        @Override
        Object invoke(Object[] a) {
            return List.copyOf(RulesFunctions.split(string(a[0]), string(a[1]), integer(a[2])));
        }
    },
    AWS_PARTITION("aws.partition", 1) {
        @Override
        Object invoke(Object[] a) {
            String region = string(a[0]);
            Partitions partitions = Partitions.defaults();
            Optional<PartitionMetadata> match = partitions.forRegion(region);
            if (match.isEmpty()) {
                throw new RuleEvaluationError("Unable to match a partition for region " + region);
            }
            PartitionMetadata partition = match.get();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", partition.id());
            result.put("dnsSuffix", partition.dnsSuffix());
            result.put("dualStackDnsSuffix", partition.dualStackDnsSuffix());
            result.put("supportsFIPS", partition.supportsFips());
            result.put("supportsDualStack", partition.supportsDualStack());
            result.put("inferred", !partitions.isKnownRegion(region));
            result.put("implicitGlobalRegion", partition.implicitGlobalRegion());
            return Collections.unmodifiableMap(result);
        }
    },
    AWS_PARSE_ARN("aws.parseArn", 1) {
        @Override
        Object invoke(Object[] a) {
            return RulesFunctions.parseArn(string(a[0]));
        }
    },
    AWS_IS_VIRTUAL_HOSTABLE_S3_BUCKET("aws.isVirtualHostableS3Bucket", 2) {
        @Override
        Object invoke(Object[] a) {
            return RulesFunctions.isVirtualHostableS3Bucket(string(a[0]), bool(a[1]));
        }
    };

    private final String id;
    private final int arity;

    LibraryFunction(String id, int arity) {
        this.id = id;
        this.arity = arity;
    }

    /** The function name used in rule sets. */
    public String id() {
        return id;
    }

    /** The number of arguments. */
    public int arity() {
        return arity;
    }

    /** The library function named {@code id}, if there is one. */
    public static Optional<LibraryFunction> fromId(String id) {
        for (LibraryFunction function : values()) {
            if (function.id.equals(id)) {
                return Optional.of(function);
            }
        }
        return Optional.empty();
    }

    /** Applies the function to evaluated arguments; the result is a rules value or {@code null} (empty). */
    public Object apply(Object[] arguments) {
        if (arguments.length != arity) {
            throw new RuleEvaluationError(id + " expects " + arity + " arguments but got " + arguments.length);
        }
        try {
            return invoke(arguments);
        } catch (ClassCastException e) {
            throw new RuleEvaluationError(id + " received an argument of the wrong type: "
                                          + Arrays.stream(arguments).map(EndpointValues::render).toList());
        }
    }

    abstract Object invoke(Object[] arguments);

    private static String string(Object value) {
        if (value instanceof String s) {
            return s;
        }
        throw new ClassCastException();
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw new ClassCastException();
    }

    private static int integer(Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        throw new ClassCastException();
    }
}
