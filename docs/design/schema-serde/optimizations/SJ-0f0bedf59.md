# Optimization experiment: smithy-java SJ-0f0bedf59

smithy-java `0f0bedf59` ("Improve perf of UserAgentPlugin", 2026-10-12): resolve the user agent app ID once per
client instead of on every call. FU-25 item, compared with racecar H9 (`Applied (re-implemented here)`, phase 2).
There is no row in `racecar-decisions.yaml` for this id, and H9's row is unchanged.

## Decision: DEFERRED-NOT-APPLICABLE

No V2 change, no measurement and no host time. V2 already does what the upstream commit adds, on both
pipelines. No candidate commit was built, so no tests or experiment ran for this id.

## What the upstream commit changes

Files: `client/client-http/src/main/java/software/amazon/smithy/java/client/http/plugins/UserAgentPlugin.java`
(+38/−31) and its `UserAgentPluginTest` (+151/−32).

1. **App ID lookup moves from each call to client build.** Before, `UserAgentInterceptor.createUa` called
   `resolveAppId(context)` (parent commit, line 127) on every request without a context app ID. That method ran
   `System.getenv("aws.userAgentAppId")` (line 130) and then a static `AWS_SDK_UA_APP_ID` value read once at class
   load (line 65). After the change, `UserAgentPlugin.configureClient` (line 42) calls `resolveDefaultAppId()`
   (line 53) once per client build and stores the result with `putConfigIfAbsent(ClientContext.APPLICATION_ID, ...)`.
   `createUa` then only reads `ClientContext.APPLICATION_ID` from the context.
2. **Two fixes to the app ID sources.** The old code read the system property name `aws.userAgentAppId` as an
   environment variable, so the system property was never used. The new code reads the system properties
   `aws.userAgentAppId` and then `sdk.ua.appId`, and then the environment variable `AWS_SDK_UA_APP_ID`. It strips
   whitespace and treats a blank value as unset.
3. **Trailing-character fix.** With an app ID and no feature IDs, the old code removed the character at
   `STATIC_SEGMENT.length() - 1` (line 121). That is the space between the static segment and `app/...`, not the
   trailing space. The new code removes the last character in every case (`b.setLength(b.length() - 1)`, line 141).

The upstream tests added cover the property order, blank values, explicit-over-default precedence, resolution per
client rather than per process, and app ID with feature IDs.

## Why nothing remains to port to V2

Citations are to `feature/pov/schema-decoupling` at `2d5af948287`.

1. **V2 resolves the app ID once per client, at build time.**
   - `SdkDefaultClientBuilder.finalizeConfiguration` registers `CLIENT_USER_AGENT` as a lazy option
     (`core/sdk-core/src/main/java/software/amazon/awssdk/core/client/builder/SdkDefaultClientBuilder.java` line
     361). `AttributeMap.Builder.build()` resolves every lazy value before the client configuration exists
     (`utils/src/main/java/software/amazon/awssdk/utils/AttributeMap.java` lines 359–365). The value is then
     cached (`DerivedValue.primeCache`).
   - The resolver, `resolveClientUserAgent` (lines 418–434), takes `USER_AGENT_APP_ID` from the client override
     configuration. When that is unset, it calls `resolveAppId` (lines 436–443, `AppIdResolver`). It then builds
     the whole client user agent string, including `app/<id>`, once
     (`SdkUserAgentBuilder.buildClientUserAgentString`, lines 56–87).
   - `AppIdResolver.resolve` (`core/sdk-core/.../internal/useragent/AppIdResolver.java` lines 50–57) reads the
     system property `sdk.ua.appId`, then the environment variable `AWS_SDK_UA_APP_ID`
     (`SdkSystemSetting.AWS_SDK_UA_APP_ID`, through `SystemSettingUtils.resolveSetting`, which trims, line 47),
     then the profile's `sdk_ua_app_id`.
   - `git grep` over every `src/main` finds no other caller of `AppIdResolver` or `AWS_SDK_UA_APP_ID`.
2. **Neither pipeline reads system properties or the environment per call for the user agent.**
   - The legacy `ApplyUserAgentStage.finalizeUserAgent` (`core/sdk-core/.../internal/http/pipeline/stages/
     ApplyUserAgentStage.java` lines 117–135) reads the resolved `SdkClientOption.CLIENT_USER_AGENT` string and
     appends the per-request parts.
   - The schema pipeline's `UserAgentHeaderCache` (racecar H9) goes further. `RequestMutationStages` creates it
     once per client configuration (`RequestMutationStages.java` line 118). Its constructor (lines 70–92)
     precomputes `head`, `tail` and `clientOnlyValue`. `headerValue` returns `clientOnlyValue` without building a
     string when the call adds no request-level value (line 147). That is the same as upstream's `DEFAULT_UA`
     constant path.
   - Neither file calls `System.getenv`, `System.getProperty` or `SdkSystemSetting`. `git grep` over both files
     and `RequestMutationStages.java` finds no match.
3. **V2 does not have the trailing-character bug.** `SdkUserAgentBuilder` appends each field followed by a space
   (`UserAgentConstant.appendFieldAndSpace`). `removeFinalWhitespace` (lines 109–113) then deletes the last
   character only when it is a space. Nothing in V2 deletes at a fixed offset. Request-level parts are appended
   after the client string with a leading space (`ApplyUserAgentStage`, `UserAgentHeaderCache.headerValue`).
4. **The source differences are V2's documented behavior, not performance.** V2 reads `sdk.ua.appId` and the
   profile setting; upstream's plugin reads `aws.userAgentAppId` first and has no profile source. Upstream also accepts a
   per-request app ID override through its context. Changing V2's sources would change what users see, and it
   would not save any per-call work. It is out of scope here.

Existing V2 tests already cover the build-time resolution and the cached header:
`core/sdk-core/src/test/java/software/amazon/awssdk/core/internal/useragent/AppIdResolutionTest.java`,
`test/codegen-generated-classes-test/src/test/java/software/amazon/awssdk/services/useragent/AppIdUserAgentTest.java`
and `core/sdk-core/src/test/java/software/amazon/awssdk/core/internal/http/UserAgentHeaderCacheTest.java`. The last
compares the cache with the legacy stage.

## FU-25 change

This smithy-java item has no row in `racecar-decisions.yaml`.

Update the `0f0bedf59` clause of FU-25 in `.kiro/specs/schema-decoupling-serde/followups.md` to: decision
DEFERRED-NOT-APPLICABLE, no V2 change, record `docs/design/schema-serde/optimizations/SJ-0f0bedf59.md`.

FU-25 was not on `feature/pov/schema-decoupling` when this record was written. The smithy-java port round's
`96eacb08575` on `port/smithy-java-1.8` adds it, and that branch is not merged yet. The clause update is applied once
FU-25 is on the feature branch, together with the other SJ items' clauses.
