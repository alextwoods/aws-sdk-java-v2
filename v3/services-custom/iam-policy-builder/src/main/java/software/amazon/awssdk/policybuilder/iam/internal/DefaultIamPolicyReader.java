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

package software.amazon.awssdk.policybuilder.iam.internal;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.policybuilder.iam.IamCondition;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyReader;
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.ArrayNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.BooleanNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.Node;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.NumberNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.ObjectNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.StringNode;

/**
 * Default implementation of {@link IamPolicyReader}.
 *
 * <p>Malformed JSON and read failures throw {@link UncheckedIOException}; a document without a value throws
 * {@link NullPointerException}; well-formed JSON that is not a policy throws {@link IllegalArgumentException} or the
 * {@link NullPointerException} of a missing required field, all as in V2.
 *
 * @see IamPolicyReader#create
 */
public final class DefaultIamPolicyReader implements IamPolicyReader {
    @Override
    public IamPolicy read(String policy) {
        return read(policy.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IamPolicy read(byte[] policy) {
        return read(new ByteArrayInputStream(policy));
    }

    @Override
    public IamPolicy read(InputStream policy) {
        Node document;
        try {
            document = PolicyJsonReader.parse(policy);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (document == null) {
            throw new NullPointerException("The policy document contains no JSON value");
        }
        return readPolicy(document);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    private IamPolicy readPolicy(Node policyNode) {
        Map<String, Node> policyObject = expectObject(policyNode, "Policy did not start with {");
        return IamPolicy.builder()
                        .version(getString(policyObject, "Version"))
                        .id(getString(policyObject, "Id"))
                        .statements(readStatements(policyObject.get("Statement")))
                        .build();
    }

    private List<IamStatement> readStatements(Node statementsNode) {
        if (statementsNode == null) {
            return null;
        }
        if (statementsNode instanceof ArrayNode array) {
            return array.elements()
                        .stream()
                        .map(n -> expectObject(n, "Statement entry"))
                        .map(this::readStatement)
                        .collect(toList());
        }
        if (statementsNode instanceof ObjectNode object) {
            return singletonList(readStatement(object.members()));
        }
        throw new IllegalArgumentException("Statement was not an array or object.");
    }

    private IamStatement readStatement(Map<String, Node> statementObject) {
        return IamStatement.builder()
                           .sid(getString(statementObject, "Sid"))
                           .effect(getString(statementObject, "Effect"))
                           .principals(readPrincipals(statementObject, "Principal"))
                           .notPrincipals(readPrincipals(statementObject, "NotPrincipal"))
                           .actionIds(readStringOrArrayAsList(statementObject, "Action", false))
                           .notActionIds(readStringOrArrayAsList(statementObject, "NotAction", false))
                           .resourceIds(readStringOrArrayAsList(statementObject, "Resource", false))
                           .notResourceIds(readStringOrArrayAsList(statementObject, "NotResource", false))
                           .conditions(readConditions(statementObject.get("Condition")))
                           .build();
    }

    private List<IamPrincipal> readPrincipals(Map<String, Node> statementObject, String name) {
        Node principalsNode = statementObject.get(name);
        if (principalsNode == null) {
            return null;
        }
        if (principalsNode instanceof StringNode string && string.value().equals(IamPrincipal.ALL.id())) {
            return singletonList(IamPrincipal.ALL);
        }
        if (principalsNode instanceof ObjectNode object) {
            List<IamPrincipal> result = new ArrayList<>();
            Map<String, Node> principalsNodeObject = object.members();
            principalsNodeObject.keySet().forEach(
                k -> result.addAll(IamPrincipal.createAll(k, readStringOrArrayAsList(principalsNodeObject, k, true)))
            );
            return result;
        }
        throw new IllegalArgumentException(name + " was not \"" + IamPrincipal.ALL.id() + "\" or an object");
    }

    private List<IamCondition> readConditions(Node conditionNode) {
        if (conditionNode == null) {
            return null;
        }
        Map<String, Node> conditionObject = expectObject(conditionNode, "Condition");
        List<IamCondition> result = new ArrayList<>();
        conditionObject.forEach((operator, keyValueNode) -> {
            Map<String, Node> keyValueObject = expectObject(keyValueNode, "Condition key");
            keyValueObject.forEach((key, value) -> {
                if (value instanceof ArrayNode array) {
                    List<String> values =
                        array.elements()
                             .stream()
                             .map(valueNode -> expectConditionValue(valueNode, "Condition values entry"))
                             .collect(toList());
                    result.addAll(IamCondition.createAll(operator, key, values));
                } else {
                    result.add(IamCondition.create(operator, key, expectConditionValue(value, "Condition value entry")));
                }
            });
        });
        return result;
    }

    private List<String> readStringOrArrayAsList(Map<String, Node> statementObject, String nodeKey,
                                                 boolean allowAccountIds) {
        Node node = statementObject.get(nodeKey);
        if (node == null) {
            return null;
        }
        if (node instanceof StringNode string) {
            return singletonList(string.value());
        }
        if (node instanceof ArrayNode array) {
            return array.elements()
                        .stream()
                        .map(n -> {
                            if (allowAccountIds) {
                                return expectStringOrAccountId(n, nodeKey + " entry");
                            }
                            return expectString(n, nodeKey + " entry");
                        })
                        .collect(toList());
        }
        throw new IllegalArgumentException(nodeKey + " was not an array or string");
    }

    private String getString(Map<String, Node> object, String key) {
        Node node = object.get(key);
        if (node == null) {
            return null;
        }
        return expectString(node, key);
    }

    private String expectString(Node node, String name) {
        Validate.isTrue(node instanceof StringNode, "%s was not a string", name);
        return ((StringNode) node).value();
    }

    private String expectStringOrAccountId(Node node, String name) {
        if (node instanceof NumberNode number) {
            return number.text();
        }
        Validate.isTrue(node instanceof StringNode, "%s was not a string", name);
        return ((StringNode) node).value();
    }

    // condition values are generally String, however in some cases they may be an AWS accountID or a boolean.
    private String expectConditionValue(Node node, String name) {
        if (node instanceof NumberNode number) {
            return number.text();
        }
        if (node instanceof BooleanNode bool) {
            return Boolean.toString(bool.value());
        }
        Validate.isTrue(node instanceof StringNode, "%s was not a string", name);
        return ((StringNode) node).value();
    }

    private Map<String, Node> expectObject(Node node, String name) {
        Validate.isTrue(node instanceof ObjectNode, "%s was not an object", name);
        return ((ObjectNode) node).members();
    }
}
