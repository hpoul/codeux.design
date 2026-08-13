Until recently every AuthPass release build started by downloading a binary
from a GitHub release. Not a pinned tag, not a checksum - just a URL, in three
per-OS variants, picked by an `if` on the runner's operating system:

```bash
curl -L -o ${tmpdir}/blackbox.go.linux.amd64 \
  https://github.com/hpoul/blackbox/releases/download/golang-v0.1-cipostdeploy/blackbox.go.linux.amd64
chmod +x ${tmpdir}/blackbox.go.linux.amd64
echo "${BLACKBOX_SECRET}" | ${tmpdir}/blackbox.go.linux.amd64 cipostdeploy -
```

That binary is my own fork of blackbox. And what it decrypted - which was
everything, there is no other mode - landed in plaintext at its real path in
the working tree and stayed there for the rest of the job: two android
keystores, four provisioning profiles, three `.p12` certificates, the Play
service account, the App Store Connect key. A build that only needed to sign
an android app bundle still had every apple credential the project owns
sitting on disk next to it.

That is now gone, and this article is about what replaced it: one
sops-encrypted file, one age key, and a CLI which hands individual credentials
to individual commands.

[TOC]

# How it got that way

Nothing in the paragraph above was a bad decision at the time. It accreted, one
reasonable step after another:

fastlane `match` needed somewhere to keep provisioning profiles, and its answer
was *another git repository*. That repository needed a deploy key. The deploy
key needed somewhere to live, and I wrote
[two](../manage-secrets-flutter-project)
[articles](../backbox-in-continuous-integration) on this
blog about the answer: encrypt it into the repo with blackbox. Blackbox has no
CI story of its own, so I forked it and added a `cipostdeploy` command. And
then the fork needed to get onto the runner somehow, which is the `curl` above.

Every one of those steps solved the problem in front of it. Seven years later
the sum is three secrets guarding one certificate, a second private repository,
Ruby on every macOS runner, and about forty lines of `curl`-and-`chmod`.

# Act one: getting fastlane out of the way

This was a separate, earlier change, but it has to come first because it is
what turned the whole thing into a file problem.

`match` was replaced with plain `xcodebuild archive` and `-exportArchive`
against a checked-in `ExportOptions.plist`, plus an ephemeral keychain the
script deletes on exit. `upload_to_testflight` and `upload_to_play_store` were
replaced with [cux_ship](https://pub.dev/packages/cux_ship), a small dart CLI
I use to ship my apps to the stores.

Two things fell out of that immediately:

* The certificate repository, its deploy key and `MATCH_PASSWORD` could all be
  deleted. Three secrets, gone, because the thing they protected is now just a
  `.p12` file.
* The apple credential could be downgraded. `match` wants to *create*
  provisioning profiles, so it needs an App Store Connect **team** key - and a
  team key cannot be scoped to one app, which means anything with access to CI
  could publish every app on the account. Without `match`, an **individual**
  key scoped to AuthPass is enough. It can upload builds. Certificates,
  identifiers and profiles all answer `401`.

What is left after that is not a signing problem anymore. It is: how do a
handful of files get onto a runner, safely, without lying around afterwards.

# Act two: one file, one key

[sops](https://github.com/getsops/sops) encrypts the *values* in a YAML file
and leaves the keys readable, so an encrypted secrets file still diffs like a
config file. [age](https://github.com/FiloSottile/age) gives it modern keypairs
instead of a GPG web of trust and a committed keyring.

Twenty-two separate `.gpg` files became one `secrets/release.yaml` at the repo
root, holding eighteen credentials grouped by family (every `<ENC>` below is an
`ENC[AES256_GCM,...]` blob in the real file):

```yaml
android:
  keystores:
    upload:   { base64: <ENC>, password: <ENC>, key_alias: <ENC> }
    amazon:   { base64: <ENC>, password: <ENC>, key_alias: <ENC> }
  play_service_account: { json_base64: <ENC> }
apple:
  api_keys:
    upload: { id: <ENC>, kind: individual, issuer_id: <ENC>, private_key_base64: <ENC> }
    team:   { id: <ENC>, kind: team,       private_key_base64: <ENC> }
  certificates:
    distribution:  { p12_base64: <ENC>, password: <ENC> }
    developer_id:  { p12_base64: <ENC>, password: <ENC> }
    mac_installer: { p12_base64: <ENC>, password: <ENC> }
  profiles:
    ios_appstore:          { base64: <ENC> }
    ios_appstore_autofill: { base64: <ENC> }
    macos_appstore:        { base64: <ENC> }
    macos_developerid:     { base64: <ENC> }
tokens:
  artifact: { env: ARTIFACT_TOKEN, value: <ENC> }
  fosshub:  { env: FOSSHUB_TOKEN,  value: <ENC> }
ssh_keys:
  github_deploy: { base64: <ENC>, env: GITHUB_DEPLOY_KEY_PATH }
placed:
  env_production: { path: authpass/lib/env/production.dart, base64: <ENC> }
  env_secrets:    { path: authpass/lib/env/secrets.dart,    base64: <ENC> }
  test_secrets:   { path: authpass/test/_testSecrets.json,  base64: <ENC> }
```

Binary credentials are base64 encoded. `path`, `env` and `kind` stay in
plaintext on purpose - more on why that is worth a paragraph in
[part two](../why-our-own-release-tool).

Who may decrypt it lives in `.sops.yaml`, so no command line ever has to repeat
it:

```yaml
creation_rules:
  - path_regex: ^secrets/.*\.yaml$
    unencrypted_regex: '^(path|env|kind)$'
    age: >-
      age1wk48cqpglu37a4zed268qxlndm0r4vxwzjwlknp0c8uk7wddduvq5gayk7,
      age18rjg79qr30r3zw9qu2fwlm4t3nuujju43me7heuy6t9rz7t9fymqyyc5cf
```

Two recipients: my own key, and one generated for this repository's CI. Not a
shared CI key across all my projects - a leak from one of them should not read
the others.

!!! note "Note"
    These are age *public* keys, and they are in the public repository already.
    Nothing here is a secret. The private identity for CI lives in the CI
    provider's secret store, mine lives in `~/.config/sops/age/keys.txt`.

# Three commands do the work

`cux_ship` has a secrets layer on top of sops and age. It shells out to the
real `sops` and the real `age` binaries, so the file stays readable by anyone
with those two tools and an identity, with or without my CLI.

**`cux_ship deps install`** downloads sops and age into `.bin/`, verifying
their checksums. This is the direct replacement for the forty lines of
per-OS `curl` at the top of this article.

**`cux_ship secrets exec`** is the one that changes how the scripts look:

```bash
cux_ship secrets exec --keystore upload --api-key upload -- ./_tools/ci-release.sh android
```

It decrypts the file, exports the value-shaped credentials as environment
variables, materialises the file-shaped ones into a temporary directory it
removes however the child exits, and runs the command at the repo root. The
credentials exist for the duration of one command instead of the duration of
one job.

**`cux_ship secrets place`** writes the three `placed:` entries to their real
paths. Those genuinely cannot come from `secrets exec`: the dart compiler reads
`lib/env/production.dart` long after any single command has ended.

**`cux_ship secrets keys`** lists what the file holds *without an identity at
all*. That is what `unencrypted_regex` buys, and it is more useful than it
sounds - you can answer "what does this project need in order to release?"
without being allowed to release.

# Scripts read the environment, and only the environment

Here is what this does to a build script. Before, `build-ios.sh` knew where
every secret lived and had to check they were all there:

```bash
SECRETS="_tools/secrets"
P12="$SECRETS/apple_distribution.p12"
P12_PASSWORD_FILE="$SECRETS/apple_distribution_p12_password"
PROFILES=("$SECRETS/ios_appstore.mobileprovision")
for f in "$P12" "$P12_PASSWORD_FILE" "${PROFILES[@]}"; do
  if [ ! -f "$f" ]; then
    echo "missing $f - decrypt the blackbox secrets first" >&2
    exit 1
  fi
done
. "$P12_PASSWORD_FILE"   # defines APPLE_DISTRIBUTION_P12_PASSWORD
```

After, it knows nothing about where anything is stored:

```bash
: "${APPLE_DISTRIBUTION_P12_PATH:?run this under 'cux_ship secrets exec'}"
: "${APPLE_DISTRIBUTION_P12_PASSWORD:?not set by secrets exec}"
P12="$APPLE_DISTRIBUTION_P12_PATH"
PROFILES=(
  "${APPLE_PROFILE_IOS_APPSTORE_PATH:?}"
  "${APPLE_PROFILE_IOS_APPSTORE_AUTOFILL_PATH:?}"
)
```

The `${VAR:?message}` idiom is worth pointing out on its own. The failure mode
"you forgot to decrypt" became "you forgot to run this under `secrets exec`",
and it is one line per variable instead of a loop.

Gradle got the same treatment. It used to read a decrypted properties file
whose *contents were filesystem paths to other secrets*, and it encoded which
keystore signs which flavour:

```groovy
def secretProperties = new Properties()
def secretPropertiesFile = rootProject.file('../_tools/secrets/gradle_home/gradle.properties')
// ...
signingConfigs {
  release { keyAlias 'authpass';   storeFile file(secretProperties['...storeFile'] ?: "invalid") }
  amazon  { keyAlias 'amazon-key'; storeFile file(secretProperties['...amazon.storeFile'] ?: "invalid") }
}
```

Two configs, because gradle was making a decision CI had already made. Now
there is one, and *which* key signs is the caller's choice:

```groovy
signingConfigs {
  release {
    keyAlias      System.getenv('ANDROID_KEY_ALIAS') ?: 'authpass'
    keyPassword   System.getenv('ANDROID_KEY_PASSWORD')
    storeFile     file(System.getenv('ANDROID_KEYSTORE_PATH') ?: "invalid")
    storePassword System.getenv('ANDROID_KEYSTORE_PASSWORD')
  }
}
```

The `"invalid"` fallback path is worth keeping, by the way. It means a fresh
checkout with no credentials at all still configures - only the release build
type is unreachable, which is exactly right for a contributor.

# The CI holds one secret

```yaml
# before
- name: Postdeploy
  env: { BLACKBOX_SECRET: "${{ secrets.BLACKBOX_SECRET_KEY }}" }
  run: authpass/_tools/postdeploy.sh
- run: ./authpass/_tools/ci-release.sh ios
```

```yaml
# after
- name: ci-install-deps
  env: { SOPS_AGE_KEY: "${{ secrets.SOPS_AGE_KEY }}" }
  run: ./authpass/_tools/ci-install-deps.sh ios
- name: ci-release
  env: { SOPS_AGE_KEY: "${{ secrets.SOPS_AGE_KEY }}" }
  run: >-
    ./authpass/_tools/ship.sh secrets exec --keystore upload --api-key upload --
    ./authpass/_tools/ci-release.sh ios
```

`SOPS_AGE_KEY` replaced `BLACKBOX_SECRET_KEY` across nine workflows, and
`postdeploy.sh` was deleted. Moving to a different CI provider now means moving
one value.

!!! note "Note"
    This is one secret for the *release credentials*, not zero secrets overall
    - a packagecloud token and the usual GitHub tokens are still there.

The `ship.sh` wrapper in those snippets earns its keep by settling three things
which were otherwise repeated at every call site: *which* dart (the one inside
the pinned flutter SDK - the runners bring none of their own, so a bare `dart`
depends on step ordering), *which* directory (`dart run` resolves the package
from the directory it runs in), and where the child process ends up
(`secrets exec` runs it at the repo root, so paths after `--` are
repo-relative). It also runs `pub get` lazily, so a workflow that needs one
credential and no build does not need an install step at all.

My favourite consequence: the job which builds the `.deb` package never builds
the app, it repackages a published tarball, so it has no flutter SDK to borrow
a dart from. It now gets `dart-lang/setup-dart` and `ship.sh deps install`, and
nothing else.

# The migration, step by step

This is the part you can actually copy. The ordering rule throughout was:
**nothing gets deleted until its replacement has driven a real build.**

1. **Get fastlane out of the way first.** Replace `match` and `build_app` with
   `xcodebuild archive` / `-exportArchive` and an ephemeral keychain. Replace
   the upload actions. Downgrade the apple credential to an app-scoped
   individual key while you are in there. Delete the certificate repository,
   its deploy key and `MATCH_PASSWORD`.
2. **Add the tool without touching the app's lockfile.** A four line wrapper
   package at `_tools/cux_ship/pubspec.yaml` with `publish_to: none` and a
   single dependency on `cux_ship: ^1.8.0`. One place to bump, and the app's
   own `pubspec.yaml` stays free of release tooling.
3. **Tell it where the app is.** AuthPass has a thin wrapper at the git root
   and all the code in `authpass/`, so it needs a `.cux-ship.yaml`:
   ```yaml
   app-dir: authpass
   ```
   A normal single-package repository needs no config file at all.
   `CHANGELOG.md` and `store/` deliberately stay at the *root* - a release
   describes what the repository shipped, not what one package did.
4. **Install sops and age**: `cux_ship deps install`.
5. **Decide who can decrypt, before encrypting anything.** Generate an age
   keypair for CI, write `.sops.yaml` with both recipients. Put the CI
   *identity* into the CI provider's secret store as `SOPS_AGE_KEY`.
6. **Move the credentials, one family at a time**, from the decrypted blackbox
   files into `secrets/release.yaml`, base64 encoding the binary ones. Verify
   each family before moving on - see the next section. Nothing is deleted
   here: blackbox stays, and the scripts read the new value *falling back to
   the old file*.
7. **Rewrite each script to read the environment**, and run each one for real
   under `secrets exec`. A real signed build, a real dry-run upload. Gradle
   moves to `System.getenv` in the same step and the two `signingConfigs`
   collapse into one.
8. **Switch the CI**: `BLACKBOX_SECRET` becomes `SOPS_AGE_KEY` in every
   workflow, each credential-needing step gets wrapped in
   `secrets exec ... --`, and `secrets place` joins the dependency-install step
   for the files the compiler reads.
9. **Delete blackbox** - `.blackbox/`, every `.gpg`, `postdeploy.sh`, the
   per-OS download. Only now, and only because the decrypted files still exist
   untracked on disk, so nothing that existed in exactly one place was
   destroyed.

# How I knew it worked

This is the part I would push back on if someone told me they had skipped it.

Migrating credentials is the one operation where "it seems to work" is not
evidence. A subtly corrupted `.p12` does not fail when you encrypt it. It fails
at signing time, weeks later, on a machine you are not sitting at, in a job
which has already burnt a build number. So three independent checks were run
before anything was encrypted, and again afterwards:

1. **Every binary value round-trips byte-identical to the original.** Not "the
   build passed" - the same bytes.
2. **`cux_ship secrets keys` reads all eighteen entries with no identity at
   all**, which proves the metadata really is readable without a key.
3. **The materialised files match the SHA of what blackbox held** - including
   the `.p8` keeping its `ApiKey_` filename prefix, because that prefix is what
   decides which claims apple is sent.

And then the removal was staged anyway: `build-ios.sh` read the environment
with a fallback to the blackbox files for as long as both existed. Blackbox
went only after the new scripts had driven a real build.

!!! danger "One thing this is not"
    Deleting the `.gpg` files from `HEAD` is not a rotation. They are still in
    the git history of a public repository, and they are exactly as safe as the
    GPG private keys are - which is the same warning I put in the
    [blackbox on CI article](../backbox-in-continuous-integration)
    seven years ago. Retiring the tool does not unpublish anything. Rotating
    the credentials themselves, at apple and google and everywhere else, is a
    separate job and an open item on my list.

# Conclusion

Roughly: twenty-two `.gpg` files became one encrypted file with eighteen
credentials. Two CI secrets and an unpinned binary download became one
`SOPS_AGE_KEY`. Credentials that used to sit at their real paths for a whole
job now live in a temp directory for the duration of one command. And the
apple credential in CI went from a key which can publish every app on the
account to one which can upload builds for AuthPass and nothing else.

The pull request was 60 files, +531 / -410, and about two thirds of that is
deletions of things which no longer need to exist.

In [part two](../why-our-own-release-tool) I write up the part that actually
took the longest: why this ended up as my own dart CLI instead of Doppler,
Vault or plain sops, which of the design decisions I went back and forth on,
and what is still broken.

!!! question "What do you think?"
    Are you still on blackbox, or did you find a different way out? Let me know
    in the comments!
