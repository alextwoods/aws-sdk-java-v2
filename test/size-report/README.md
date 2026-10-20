# size-report

Builds the Size_Report (Requirement 36) and checks it against `docs/design/schema-serde/size-report.md`. The report
compares the Baseline_Build with the feature build for each reported Prototype_Service_Set service: compressed JAR
bytes, uncompressed class bytes, class count, bytes per package, the seven-category breakdown of Requirement 36.2,
and the runtime module JARs (Requirement 36.6). The report's Method section documents the category rules and the
ASM attribution.

## Baseline_Build

The Baseline_Build is the merge base of this branch (`fc8b74e9fe21`), built from source with the same JDK, Maven
flags and compiler options as the feature build (Requirement 36.7). The released `2.49.6` JAR is not used: it was
built by the release pipeline with JDK 17.

```bash
scripts/schema-serde/build-size-baseline.sh            # all seven prototype services by default
```

The script checks the commit out into a worktree under `$TMPDIR`, runs
`mvn -o clean install -P quick -pl <modules> -am` there with its own local repository (`maven.repo.local.tail`
reads the shared one), and writes `~/.cache/aws-sdk-java-v2/size-baseline/fc8b74e9fe21/`:

- `jars/<artifactId>.jar`: each service JAR and its `software.amazon.awssdk` runtime dependencies
- `build-info.properties`: commit, JDK, Maven version and flags, and the effective compiler options

It fails if the effective `maven-compiler-plugin` configuration differs between the commit and the current checkout.
Rerun it after changing JDK. Point the build at another directory with `-Dsize-report.baselineDir=<dir>`.

## Check and regenerate

Build the reported services first (checkpoint 6 builds them with `-P quick --am`), then:

```bash
mvn clean install -pl :size-report                            # fails if size-report.md is out of date
mvn clean install -pl :size-report -Dsize-report.update=true  # rewrites size-report.md
```

`SizeReportDocumentTest` fails if the Baseline_Build is missing, if its JDK differs from the JDK running the build,
or if the class files of the two builds show different compiler options. Any change to a reported service's
generated code changes the report, so regenerate and commit `size-report.md` with such changes.

## Adding services

The report covers the seven Prototype_Service_Set services (`size-report.services` in `pom.xml`). To add one:

1. Add the service as a `test`-scoped dependency in `pom.xml` and to `size-report.services`
   (`artifactId=Display name`, comma-separated, report order; the display name is the one
   `disabled-customizations.md` uses).
2. Rebuild the baseline with every reported service: `scripts/schema-serde/build-size-baseline.sh --modules
   <all reported services>`.
3. Regenerate the report.

## Size-reduction opportunities

`ReductionOpportunity` lists the further reductions of Requirement 36.5. Each one selects the feature build code it
would remove (whole classes, or members of `model` classes), and the report measures that code in every service JAR.
Add an opportunity there, not in `size-report.md`.
