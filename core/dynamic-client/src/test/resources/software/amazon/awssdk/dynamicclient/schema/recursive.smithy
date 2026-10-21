$version: "2.0"

namespace test.rec

structure SelfRef {
    name: String
    next: SelfRef
}

structure MutualA {
    b: MutualB
}

structure MutualB {
    a: MutualA
    name: String
}

structure TreeNode {
    children: TreeNodeList
}

list TreeNodeList {
    member: TreeNode
}

structure MapNode {
    children: MapNodeMap
}

map MapNodeMap {
    key: String
    value: MapNode
}

union Expr {
    literal: Integer
    negate: Expr
    sum: ExprList
}

list ExprList {
    member: Expr
}

/// Reaches every recursive group above, so that one conversion builds them all.
structure Root {
    self: SelfRef
    mutual: MutualA
    tree: TreeNode
    map: MapNode
    expr: Expr
}
