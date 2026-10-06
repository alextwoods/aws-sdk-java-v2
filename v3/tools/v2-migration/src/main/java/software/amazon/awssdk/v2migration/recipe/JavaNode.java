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

package software.amazon.awssdk.v2migration.recipe;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * The attributed Java syntax of a source item: the compiler's tree path to the construct and the compiler utilities
 * that resolve its symbols and types against the migrated project's class path. Valid only during the
 * {@link MigrationRecipe#visit} call it is passed to.
 *
 * @param unit     the compilation unit
 * @param path     the tree path to the item's construct
 * @param trees    tree and element utilities of the compilation
 * @param elements element utilities
 * @param types    type utilities
 */
public record JavaNode(CompilationUnitTree unit, TreePath path, Trees trees, Elements elements, Types types) {

    /** The item's tree. */
    public Tree tree() {
        return path.getLeaf();
    }

    /** The start offset of {@code tree} in the file text. */
    public int start(Tree tree) {
        return (int) positions().getStartPosition(unit, tree);
    }

    /** The end offset (exclusive) of {@code tree} in the file text. */
    public int end(Tree tree) {
        return (int) positions().getEndPosition(unit, tree);
    }

    private SourcePositions positions() {
        return trees.getSourcePositions();
    }
}
