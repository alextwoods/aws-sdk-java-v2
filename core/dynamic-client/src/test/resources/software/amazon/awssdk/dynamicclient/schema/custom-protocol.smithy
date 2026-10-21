$version: "2.0"

namespace test.custom

/// A protocol definition whose traits list adds a trait that has no typed runtime class.
@trait(selector: "service")
@protocolDefinition(traits: [customMeta])
structure customProtocol {}

@trait
structure customMeta {
    name: String
    small: Integer
    big: Long
    huge: BigInteger
    ratio: Double
    items: MetaItems
}

list MetaItems {
    member: String
}

/// Not in any protocol's traits list, so never included.
@trait
string unlisted

@customProtocol
service CustomService {
    operations: [Ping]
}

operation Ping {
    input := {
        @customMeta(name: "n", small: 1, big: 5000000000, huge: 99999999999999999999, ratio: 1.5, items: ["x", "y"])
        @unlisted("u")
        value: String
    }
}
