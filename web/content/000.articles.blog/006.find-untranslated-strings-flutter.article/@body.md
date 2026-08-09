Localizing an app is usually treated as a one-off task. You sit down before the
first translated release, sweep every screen, move every user facing string into
`.arb` files, and ship it. Done.

Except it is not done. Over the following months new strings arrive one pull
request at a time — a `Text('Save')` in a hastily added dialog, an error message
someone inlined while debugging, a tooltip added at the end of a long review.
None of them are noticeable on their own. Six months later a translator asks why
half of the new settings screen is still in English.

Localization is a ratchet, and the ratchet only holds if something checks it on
every commit.

# Why grepping for quotes does not work

The obvious approach — search the code for quoted strings — fails immediately,
because most string literals in a Dart codebase are not user facing:

```dart
final prefs = await SharedPreferences.getInstance();
await prefs.setString('lastOpenedFile', path);

_logger.fine('Opening file $path');

const platform = MethodChannel('app.authpass/autofill');

final json = {'version': 2, 'entries': entries};
```

Map keys, log messages, method channel names, asset paths, route names,
generated code. In a real app these outnumber the translatable strings by a wide
margin. A checker that reports all of them produces hundreds of warnings on the
first run, everyone agrees it is noise, and it gets switched off within a day.

So the interesting engineering problem is not *finding* string literals. It is
*suppressing* the ones that are legitimately not translatable — and doing it in
a way that stays honest as the code changes.

# The tool

[string_literal_finder](https://pub.dev/packages/string_literal_finder) is what I
have been using for this, in AuthPass and elsewhere. It works in two modes, and
you want both.

## As a command line tool

```shell
$ dart pub global activate string_literal_finder
$ dart pub global run string_literal_finder --path=example
2020-08-08 15:11:31.273227 INFO string_literal_finder - Found 1 literals:
2020-08-08 15:11:31.274592 INFO string_literal_finder - lib/example.dart:17:30 'not translated'
Found 1 literals in 1 files.
```

It exits with status `1` when it finds anything, which is all you need to turn it
into a build gate.

## As an analyzer plugin

More useful day to day: the same checks run inside the analysis server, so the
warnings appear as squiggles while you type, in whatever editor you use.

Add it as a dev dependency:

```shell
flutter pub add --dev string_literal_finder
```

and enable the plugin in `analysis_options.yaml`:

```yaml
analyzer:
  plugins:
    - string_literal_finder

string_literal_finder:
  exclude_globs:
    - '**/*.g.dart'
    - '**/*.freezed.dart'
```

Then restart the analysis server. This matters more than the CI check does,
because a warning you see while writing the line costs nothing to fix, and a
warning you see twenty minutes later in CI costs a context switch.

# The escape hatches

This is the part that decides whether the tool survives contact with a real
codebase. There are a few different ways to say "this string is fine", and the
right one depends on how big the exception is.

Some are built in and need no annotation at all. Anything passed to a `Logger`
from the [logging](https://pub.dev/packages/logging) package is ignored, and so
is anything in a file matched by `exclude_globs`. Between them that removes a
large share of the noise before you write a single annotation.

For everything else there is `@NonNls`, from the companion
`string_literal_finder_annotations` package. The name is borrowed from IntelliJ,
which has had exactly this annotation for Java for years.

```dart
import 'package:string_literal_finder_annotations/string_literal_finder_annotations.dart';

// A single parameter: callers may pass literals for `key`, but not for `label`.
void track(@NonNls String key, String label) {}

// A whole function: every literal inside is ignored.
@NonNls
String _storageKeyFor(String id) => 'entry:$id';

// An inline expression, for things like map literals.
final config = nonNls({
  'version': 2,
  'format': 'kdbx',
});

// A single line, when nothing else fits.
final route = '/entry/edit'; // NON-NLS
```

What I like about this design is that the exceptions are *explicit and local*.
The annotation sits next to the string it excuses, so it shows up in review, and
a reviewer can ask "is this really not user facing?" at the moment the decision
is made. Compare that with a central ignore list, which nobody ever reads again
after it is added to.

The plugin also offers a quick fix to pull a literal out into your `.arb` file
directly from the editor, which removes most of the friction from doing the
right thing.

# Wiring it into CI

The command line mode is what you run on every build. Since it already exits
non-zero on findings, the minimal version is one step:

```yaml
- name: Check for untranslated strings
  run: |
    dart pub global activate string_literal_finder
    dart pub global run string_literal_finder --path=.
```

There are a few options worth knowing about:

* `--exclude-path` and `--exclude-suffix` for excluding paths from the command
  line, matched with `startsWith` and `endsWith` respectively.
* `--annotations-output-file` writes findings in the format used by
  [annotations-action](https://github.com/Attest/annotations-action/), so the
  results show up inline on the pull request instead of buried in a log.
* `--metrics-output-file` writes a small JSON summary:

  ```json
  {
    "stringLiterals": 0,
    "stringLiteralsFiles": 0,
    "filesAnalyzed": 214,
    "filesSkipped": 31,
    "filesWithoutLiterals": 214
  }
  ```

That last one is more useful than it looks if you are adding this to an existing
app. Going from several hundred findings to zero in a single pull request is not
realistic. Recording `stringLiterals` on every build lets you treat it as a
number that is only ever allowed to go down, and fix the backlog gradually
without blocking everyone in the meantime.

# Worth it?

The check itself is unglamorous and takes an afternoon to set up. What you get
is that "we support twelve languages" stays true without anyone having to
remember it — which, for something that otherwise degrades silently and only
surfaces as a complaint from a user in a language you do not read, is a good
trade.
