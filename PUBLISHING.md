# Publishing Klocale

Step-by-step to release Klocale to Maven Central and verify it before announcing.
Do these in order. Do NOT promote the library until step 7 passes.

## 0. Confirm coordinates
- The Maven group is `io.github.andreadellaporta01` (see `gradle.properties` → `GROUP`).
  If your GitHub handle differs, fix `GROUP`, `POM_URL`, `POM_SCM_*`, `POM_DEVELOPER_*`
  first — everything below depends on it.

## 1. Create the GitHub repo and push
```bash
cd ~/klocale
gh repo create klocale --public --source=. --remote=origin --push
# or create it in the UI and: git remote add origin ... && git push -u origin main
```

## 2. Verify the namespace on Central Portal
- Sign in at https://central.sonatype.com with GitHub.
- Add namespace `io.github.andreadellaporta01`. For `io.github.*` namespaces,
  verification is done by proving you own the GitHub account (create the public
  repo / temporary verification repo the portal asks for). Wait for "Verified".

## 3. Generate a Central Portal publishing token
- Central Portal → Account → Generate User Token. You get a username/password pair.
- These become `mavenCentralUsername` / `mavenCentralPassword`.

## 4. Create a GPG signing key
```bash
gpg --gen-key                                   # note the KEY_ID
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>   # publish the public key
gpg --export-secret-keys --armor <KEY_ID>       # this armored block is signingInMemoryKey
```

## 5. Bump the version (Central rejects -SNAPSHOT)
- In `gradle.properties`, set `VERSION_NAME=0.1.0` (no `-SNAPSHOT`).

## 6. Publish
Local (secrets via env), or push a `v*` tag to run `.github/workflows/publish.yml`
(add the four secrets to the GitHub repo first: `MAVEN_CENTRAL_USERNAME`,
`MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, `SIGNING_IN_MEMORY_KEY_PASSWORD`).

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=...
export ORG_GRADLE_PROJECT_mavenCentralPassword=...
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor <KEY_ID>)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
```

## 7. VERIFY before announcing (do not skip)
After ~15–30 min indexing, in a *fresh* throwaway project with only `mavenCentral()`:
```kotlin
implementation("io.github.andreadellaporta01:klocale-core:0.1.0")
implementation("io.github.andreadellaporta01:klocale-compose:0.1.0")
```
Confirm it resolves and a `formatCurrency(...)` call works. Only now announce.

## After publishing
- Tag the release on GitHub with notes.
- Then (and only then) publish the announcement post.
- For the next version, bump `VERSION_NAME` again.
