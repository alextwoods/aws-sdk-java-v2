$version: "2"

namespace software.amazon.awssdk.test.codegen.lifecycle

/// Fixture service used to verify the aws-codegen Maven lifecycle, static operation descriptor generation and schema
/// generation. It covers an operation with input and output, one without input, a resource-bound operation, a
/// streaming operation, operation errors and service errors; self-recursive and mutually recursive structures, lists
/// and unions; an enum and an intEnum; nested lists and maps, sensitive values, blobs and timestamps; defaults,
/// constraints and unknown traits. aws-codegen requires a supported
/// protocol and modeled auth for every operation.
@aws.protocols#awsJson1_0
@aws.auth#sigv4(name: "codegenlifecyclefixture")
@fixture.meta#owner("codegen-lifecycle")
service CodegenLifecycleFixture {
    version: "2026-01-01"
    operations: [Ping, GetStatus, Upload]
    resources: [Widget]
    errors: [ServiceUnavailable]
}

operation Ping {
    input := {
        message: String
    }
    output := {
        message: String
    }
}

/// Has no input, so its input is smithy.api#Unit.
@readonly
operation GetStatus {
    output := {
        status: String
    }
}

/// Streams its input body.
operation Upload {
    input := {
        @required
        name: Label

        @required
        body: Body
    }
}

resource Widget {
    identifiers: { widgetId: String }
    read: GetWidget
}

@readonly
operation GetWidget {
    input := {
        @required
        widgetId: String
    }
    output := {
        @required
        widgetId: String
        name: String
        tree: WidgetTree
        outline: WidgetShape
        color: Color
        size: Size
        limits: Limits
        details: WidgetDetails
    }
    errors: [WidgetNotFound]
}

/// A tree of widgets: recursive through its own parent member and through a list.
structure WidgetTree {
    label: Label
    children: WidgetTreeList
    @fixture.meta#note({ depth: [1, 2.5], exact: true })
    parent: WidgetTree
}

list WidgetTreeList {
    member: WidgetTree
}

/// A union that nests itself and the tree.
union WidgetShape {
    circle: Radius
    nested: WidgetShape
    tree: WidgetTree
    none: Unit
}

@fixture.meta#owner("paint")
@fixture.meta#reviewed
enum Color {
    @fixture.meta#note("primary")
    @fixture.meta#owner("paint")
    RED = "red"

    GREEN = "green"
}

intEnum Size {
    SMALL = 1

    @fixture.meta#note(null)
    LARGE = 10
}

structure Limits {
    @range(min: 0, max: 100)
    percent: Integer = 50

    @required
    @length(min: 1, max: 8)
    code: Label

    flags: FlagList

    enabled: Boolean = true
}

/// Exercises the generated-type state rules: several required members, a required member with a default, enum
/// collections, nested lists and maps, a sensitive value and collection, a blob and a timestamp.
structure WidgetDetails {
    @required
    owner: String

    @required
    serial: Long

    @required
    revision: Integer

    @required
    ratio: Double = 1.5

    secret: Secret

    secretsByName: SecretMap

    colors: ColorList

    colorsByName: ColorMap

    sizes: SizeList

    matrix: LabelMatrix

    tagsByGroup: TagGroups

    thumbnail: Blob

    createdAt: Timestamp

    annotation: WidgetAnnotation
}

/// A required document, which Smithy client error correction leaves null (and treats as present).
structure WidgetAnnotation {
    @required
    body: Document

    note: String
}

@sensitive
string Secret

map SecretMap {
    key: String
    value: Secret
}

list ColorList {
    member: Color
}

map ColorMap {
    key: String
    value: Color
}

list SizeList {
    member: Size
}

list LabelMatrix {
    member: StringList
}

list StringList {
    member: String
}

map TagGroups {
    key: String
    value: StringList
}

@pattern("^[a-z]+$")
@length(min: 1)
string Label

@range(min: 1)
double Radius

@uniqueItems
list FlagList {
    member: Boolean
}

@streaming
blob Body

@error("client")
structure WidgetNotFound {
    message: String

    widgetId: String
}

@error("server")
@retryable
structure ServiceUnavailable {
    message: String
}
