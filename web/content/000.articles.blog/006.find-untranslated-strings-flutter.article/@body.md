Localizing a flutter app is usually treated as a one-off task. Before the first
translated release you go through every screen, move all user facing strings
into `.arb` files and ship it. Done.

Except it is never done. Over the next months new strings sneak in one pull
request at a time - a `Text('Save')` in a quickly added dialog, an error message
someone inlined while debugging, a tooltip added at the end of a long review.
None of them is noticeable on its own. Half a year later a translator asks why
half of the new settings screen is still in english.

So what you really need is something which checks on every commit that no
untranslated strings were added.

# Why a simple grep does not work

The obvious approach would be to simply search the code for quoted strings.
Unfortunately this fails immediately, because most string literals in a dart
codebase are not user facing:

```dart
final prefs = await SharedPreferences.getInstance();
await prefs.setString('lastOpenedFile', path);

_logger.fine('Opening file $path');

const platform = MethodChannel('app.authpass/autofill');

final json = {'version': 2, 'entries': entries};
```

Map keys, log messages, method channel names, asset paths, route names,
generated code. In a real app those outnumber the translatable strings by far.
A checker which reports all of them produces hundreds of warnings on the first
run, everyone agrees it is just noise, and it is switched off again within a
day.

So the hard part is not *finding* string literals. It is ignoring the ones
which are legitimately not translatable - in a way which still holds up while
the code keeps changing.

# string_literal_finder

[string_literal_finder](https://pub.dev/packages/string_literal_finder) is a
package I wrote for exactly this problem. You can use it in two ways, and I
would recommend both.

## As a command line tool

```shell
$ dart pub global activate string_literal_finder
$ dart pub global run string_literal_finder --path=example
2020-08-08 15:11:31.273227 INFO string_literal_finder - Found 1 literals:
2020-08-08 15:11:31.274592 INFO string_literal_finder - lib/example.dart:17:30 'not translated'
Found 1 literals in 1 files.
```

It exits with status `1` as soon as it finds any literals, which is all you
need to use it as a check on your CI.

## As an analyzer plugin

Even more useful during development: the same checks can run inside the
analysis server, so the warnings show up directly in your editor while you
type.

Add it as a dev dependency:

```shell
flutter pub add --dev string_literal_finder
```

and enable the plugin in your `analysis_options.yaml`:

```yaml
analyzer:
  plugins:
    - string_literal_finder

string_literal_finder:
  exclude_globs:
    - '**/*.g.dart'
    - '**/*.freezed.dart'
```

Now restart the analysis server and you should see the warnings right in your
editor. 🎉️

In my experience this matters even more than the CI check. A warning you see
while writing the line costs nothing to fix. A red CI build twenty minutes
later costs a context switch.

# Ignoring strings which should not be translated

This is the part which decides whether such a tool survives in a real
codebase. There are a few ways to say "this string is fine", depending on how
big the exception is.

Some strings are ignored automatically without any annotation: everything
passed to a `Logger` of the [logging](https://pub.dev/packages/logging)
package, and every file matched by your `exclude_globs`. This alone removes a
big part of the noise.

For everything else there is `@NonNls` from the companion
`string_literal_finder_annotations` package. (The name is borrowed from
IntelliJ, which has had exactly this annotation for Java for years.)

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

What I like about this: the exception sits directly next to the string it
excuses. So it shows up in code review, and a reviewer can simply ask "is this
really not user facing?" at the moment the decision is made. With a central
ignore list nobody ever looks at it again once it is added.

The analyzer plugin also provides a quick fix to move a literal into your
`.arb` file directly from the editor, which removes most of the friction of
doing the right thing.

# Running the check on your CI

The command line tool is what you run on every build. Since it already exits
non-zero when it finds something, the minimal version is a single step:

```yaml
- name: Check for untranslated strings
  run: |
    dart pub global activate string_literal_finder
    dart pub global run string_literal_finder --path=.
```

A few options worth knowing about:

* `--exclude-path` and `--exclude-suffix` to exclude paths from the command
  line, matched with `startsWith` and `endsWith` respectively.
* `--annotations-output-file` writes the findings in the format used by
  [annotations-action](https://github.com/Attest/annotations-action/), so they
  show up inline on the pull request instead of buried in a log.
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

The last one is more useful than it looks if you add the check to an existing
app. Going from a few hundred findings to zero in one pull request is not
realistic. But if you record the `stringLiterals` count on every build you can
treat it as a number which is only allowed to go down, and fix the backlog bit
by bit without blocking anyone in the meantime.

# Conclusion

The check itself is unglamorous and takes maybe an afternoon to set up. What
you get for it: "we support twelve languages" stays true without anyone having
to remember it. Otherwise untranslated strings degrade silently, and the first
one to notice is a user complaining in a language you can not read.

If you run into problems or have questions, let me know in the comments!
