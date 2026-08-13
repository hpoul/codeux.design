There are four or five well established answers to the question "how do secrets
get to a build server", all of them maintained by people with more time for it
than I have. Picking your own is usually the wrong call. AuthPass now releases
with a dart CLI I maintain myself, so this is the part of the story where I
have to justify that.

[Part one](../replacing-blackbox-with-sops-and-age) covered the mechanics of
moving off blackbox and fastlane: twenty-two `.gpg` files became one
sops-encrypted YAML file, build scripts stopped knowing where secrets live, and
CI went down to a single `SOPS_AGE_KEY`. This is the *why* - what each of the
alternatives actually leaves you holding, which decisions I went back and forth
on, and what is still broken.

[TOC]

# What each alternative leaves you holding

## fastlane

fastlane earns its keep at the store API layer, and if you are shipping an app
today it is still the default answer for good reasons. The price is a ruby
toolchain on every runner, for a release path that runs a handful of times a
month, plus the gem drift and dependency bumps that come with it.

The specific thing I wanted out of, though, is `match`. It answers "where do
certificates live?" with *another git repository*, which then needs a deploy
key, which then needs a password - three secrets guarding one certificate. And
because `match`'s model is that it can *create* profiles, it wants an App Store
Connect team key, which cannot be scoped to a single app.

That is a lot of blast radius in exchange for not having to think about
signing. Once you accept that a `.p12` and a `.mobileprovision` are just files,
`xcodebuild` will take them directly and the entire structure above is
unnecessary.

## blackbox, git-crypt, plain GPG

Encrypting secrets into the repository is the right instinct, and I still
think so after moving off it. The material is versioned, it shows up in review,
and it works on a plane.

What breaks down is everything *above* the file. Decryption is all-or-nothing
and lands plaintext in the working tree. Key distribution is a committed GPG
keyring. And there is no CI story at all - which is precisely why AuthPass was
downloading a personally maintained fork of blackbox, unpinned, from a GitHub
release, in three per-OS variants, on every single build.

## Plain sops and age

This fixes encryption, key distribution and reviewability completely, and it is
the actual foundation here. `cux_ship` shells out to the real `sops` and the
real `age`. Anyone with those two binaries and an identity can read
`secrets/release.yaml` without my tool existing.

What plain sops does *not* give you is the layer every release script was
hand-rolling anyway:

* base64-decode this value into a temporary file, and export *that* one as an
  environment variable,
* hand the Play client the service account JSON rather than a path to it,
* clean all of it up on every exit path, including the ones where the build
  crashed,
* and fail loudly, by name, when a value is missing.

That is maybe two hundred lines of bash, and it is exactly where the bugs live.

## Hosted secret managers

Doppler, Vault, 1Password, or just GitHub Secrets on its own. These are good
products and for a team they are very likely the right answer. What they do is
move the trust to a vendor and the configuration into a provider-specific UI.

Concretely, for this project: a `.p12`, its passphrase and the matching
provisioning profile stop being one reviewable object. A change to them does
not show up in a diff. Signing a build on a laptop needs a network round trip
and an account. And "which of my two keystores is this, and which alias does it
use?" has no schema behind it - it is three loose environment variables that
happen to be named similarly.

## So: a small dart CLI

Dart is already free on every runner in this project. The pinned flutter SDK
gets installed regardless, which is precisely the step ruby had to be added
*alongside*. The same tool ships my other apps, so the schema is shared instead
of reinvented per repository. And a wrapper `pubspec.yaml` in `_tools/` keeps
the release tooling's dependency tree out of the app's own lockfile.

That is the whole argument. It is not "the alternatives are bad", it is "the
marginal cost of this one was close to zero, and it removed a language
runtime".

# The decisions I went back and forth on

## Leaving `path`, `env` and `kind` unencrypted

```yaml
unencrypted_regex: '^(path|env|kind)$'
```

This one took the longest, because it is a deliberate disclosure.

`path`, `env` and `kind` are metadata, not credentials, and two things need
them readable without a key. `secrets keys` reports what a file holds without
decrypting it. And `secrets place` checks a path against the repository - is it
gitignored, is it tracked, does it escape the tree - *before* writing a single
byte. Encrypt those fields and both checks only work for someone who already
holds a key, which is the exact property they exist to avoid.

What it costs: a path tells you where the project keeps things, and an env name
tells you which services it talks to. Both were already visible in
`.gitignore` and in the release scripts, so this leaks nothing new. But there
is an invariant to hold on to, and it is easy to break by accident: **no field
carrying a secret may ever be named `path`, `env` or `kind`.**

## The notarization key is deliberately not in the file

`notarytool` refuses the app-scoped individual key with a `401`. It accepts
only a team key - which, as above, cannot be limited to one app.

So notarization is not automated. It runs from my laptop, with a key that lives
in `~/.appstoreconnect/private_keys` and is not in the repository in any form,
encrypted or otherwise.

I went around this a few times looking for a way to make it work, and the
conclusion I landed on is the one worth writing down: a secrets system is only
as good as what you decline to put into it. Encrypting an admin credential into
a public repository is still putting an admin credential into a public
repository.

## The tool refuses to guess

The file holds two keystores and two API keys, so every call site has to name
which one it means:

```bash
cux_ship secrets exec --keystore upload --api-key upload -- ...
```

It will materialise what it is told to and will not pick between two of a kind,
even in a build that would only ever have read one of them. Slightly annoying
to type; considerably less annoying than discovering months later that the
amazon keystore signed a Play upload.

## Absent is not the same answer as empty

The Play and TestFlight uploads pass an explicitly empty release-notes file.
"Leave the store listing alone" has to be said out loud, because left to infer
it, the tool would have read the root `CHANGELOG.md` and started publishing
release notes that no previous release had ever published.

## Fork pull requests still build

No `SOPS_AGE_KEY` means `secrets place` is skipped, and the handful of tests
that need `_testSecrets.json` are the ones that skip with it. A contributor is
not blocked by a credential they cannot have, and does not get a red build for
it either.

# A bug this turned up by accident

While rewriting the ios signing script I found that the temporary keychain's
partition list did not include `codesign:`. Which means `codesign` could pop a
GUI permission prompt.

On a laptop that is a dialog you click. On a CI runner it is a prompt nobody
can answer, and a build that hangs until the job times out. It had never
actually been hit, because the keychain is created and used in the same
process, so the prompt was suppressed by a coincidence of ordering.

That is about as archetypal a silent CI failure as they come, and it is a
decent argument on its own for touching this code at all. Nobody finds that bug
by reading a script that has been passing for years.

# Rolling your own means owning the bug reports

The obvious counterpoint to everything above, and it is a real one.

`cux_ship` 1.7.1 was a security fix in exactly this area. A malformed decrypted
secrets file could print its own contents: a YAML parse error renders the
offending source line as part of the exception message, and the whole exception
was being interpolated into stderr. So a stray tab character in a decrypted
secret would have put a credential into a CI log.

I would rather tell that story than not, because it is the most honest
paragraph available here. It is precisely the class of bug a secrets tool
exists in order to not have, it existed for a while, and it was mine to fix.
A hosted secret manager has a team whose whole job is finding that before you
do.

# What is still open

Since the point of this article is the honest version:

* **Nothing has been rotated.** The old `.gpg` blobs are still in the git
  history of a public repository, and deleting them from `HEAD` changed
  nothing about that. Rotating the actual credentials at apple, google and
  everywhere else is manual work I have not done yet. `sops updatekeys`
  re-encrypts the data key to a new recipient set - it does not rotate what the
  file protects.
* **An encrypted blob in a public repository is offline-crackable forever.**
  age's cryptography is fine and that is not the concern; the point is that
  "revoke access" is not a thing you can do retroactively to a git history.
* **The apple upload does not run on CI yet.** iOS and macOS uploads have only
  been driven from my laptop. Android is the only store CI has actually
  uploaded to. There is a known issue where `altool` looks for
  `AuthKey_<KEYID>.p8` in its own search paths and does not respect
  `APPLE_API_PRIVATE_KEY_PATH`, which ends in
  `ERROR: [altool] Failed to load AuthKey file. (-43)`.
* **Notarization stays a laptop operation**, on purpose, as above.
* **fastlane is not gone from the repository.** The release path no longer uses
  it, but `android/fastlane/metadata/` still exists as store listing metadata,
  which is a perfectly good use of it.
* **The docs lag the code.** There are still places in the repository which
  describe the blackbox setup.

# Conclusion

If you are shipping a flutter app and looking at this whole area, my actual
recommendation is not "write your own tool". It is: **sops with age is a very
good default**, it is better than blackbox on every axis I care about, and
migrating to it is a weekend of careful work with a clear verification story.

The custom CLI on top is worth it when you are already running dart on every
runner, you ship more than one app, and you keep writing the same two hundred
lines of decode-export-cleanup bash. If none of those are true for you, plain
`sops exec-env` and a handful of scripts will get you most of the way, and the
encrypted file will look exactly the same.

!!! question "What do you think?"
    Especially interested if you went the other way - hosted secret manager,
    or stayed on fastlane match and are happy with it. Let me know in the
    comments!
