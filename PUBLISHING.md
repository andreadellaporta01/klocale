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
This authenticates the upload. It is NOT your website password — it's a machine credential.

1. central.sonatype.com → top-right account menu → **View Account**.
2. Click **Generate User Token**.
3. If asked for a **token name**, type any label for your own reference (e.g.
   `klocale-publish`) — it does not go into the build. If asked for an **expiration**,
   pick a long window (or "no expiration"); when it expires, publishing fails until you
   regenerate and update the credentials below.
4. On confirm it shows a **username** and **password** (random strings). Copy both now —
   the password is shown only once.

These two values map to:
- `mavenCentralUsername` = the token username
- `mavenCentralPassword` = the token password

## 4. Create a GPG signing key
Central requires every artifact to be signed, and the signature must verify against your
public key on a keyserver.

**Use RSA, not ed25519.** Central's validation is battle-tested with RSA; newer ECC keys
(ed25519) sometimes fail signature verification. Save yourself a wasted publish attempt.

```bash
# 1. Generate the key — choose: (1) RSA and RSA, 4096 bits, an expiration you like.
#    You'll be asked for a name/email and a passphrase. The passphrase is
#    signingInMemoryKeyPassword — save it in your password manager.
gpg --full-generate-key

# 2. Find your KEY_ID (the hex after the algorithm, e.g. rsa4096/ABCD1234...):
gpg --list-secret-keys --keyid-format=long

# 3. Publish the PUBLIC key to a keyserver Central checks:
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>

# 4. Export the PRIVATE key — the whole armored block (-----BEGIN ... END-----)
#    is signingInMemoryKey:
gpg --export-secret-keys --armor <KEY_ID>
```

**If `--send-keys` fails with `keyserver send failed: Invalid argument`** (a common
GnuPG/dirmngr issue on macOS, even with `hkps://`), skip gpg and upload the public key
over plain HTTP instead — it's what gpg does under the hood:

```bash
gpg --export --armor <KEY_ID> > pubkey.asc
curl -X POST https://keyserver.ubuntu.com/pks/add --data-urlencode "keytext@pubkey.asc"
# verify it's retrievable (should print a PGP PUBLIC KEY BLOCK):
curl "https://keyserver.ubuntu.com/pks/lookup?op=get&options=mr&search=0x<KEY_ID>"
```

Or paste the contents of `pubkey.asc` into the "Submit Key" box at
https://keyserver.ubuntu.com.

Credentials produced here:
- `signingInMemoryKey` = the full armored private-key block from step 4
- `signingInMemoryKeyPassword` = the passphrase you set in step 1

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
