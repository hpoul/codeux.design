For every flutter mobile app there are certain secrets which have to be
integrated into the app. These might include API Keys or private keys
for services like Firebase, Analytics, etc. Or provisioning profiles for 
iOS.

This is the first part of what will become three parts on how
to build flutter apps on a CI.

1. Setting up Blackbox & Managing Signing Keys (This article)
2. Using Blackbox on a CI (Coming soon)
3. Building release builds on Travis (Android) and Cirrus CI. (iOS)
   (Coming soon)

# Blackbox Overview

It is generally not a good idea to add plaintext API keys, passwords, etc.
to your git repository. But it is very convenient. This is where
[Blackbox](https://github.com/StackExchange/blackbox) comes in and allows
you to store encrypted secrets along side your flutter app.

Possible secrets could include:
    
* Upload or signing keys.
* Secret API Keys for services used in the app
  (like Google Analytics, etc.).
* Provisioning Profiles or similar.


# Installation & Set Up

[Blackbox](https://github.com/StackExchange/blackbox) are a collection of
shell scripts based on GPG to store secrets inside public git repositories.

Installation is as easy as cloning the git repository and adding it's
`bin/` directory to your `$PATH`. I would recommend cloning my fork
because I've added a useful `blackbox_create_role_account` script.

```bash
git clone --branch create_role_account https://github.com/hpoul/blackbox/
export PATH=$(pwd)/blackbox/bin
```

Afterwards to get started you have to [set up and create a GPG signing key][1].

Now change into your git repository and we'll add blackbox. Make sure to 
exchange `my@email.com` with your actual email address which 
includes your GPG key.

[1]: https://help.github.com/en/articles/generating-a-new-gpg-key

```console
bash$ blackbox_initialize
> Enable blackbox for this git repo? (yes/no) yes
>  VCS_TYPE: git

bash$ blackbox_addadmin my@email.com
gpg: keybox '/myrepo/.blackbox/pubring.kbx' created
gpg: /myrepo/.blackbox/trustdb.gpg: trustdb created
gpg: key 3F58C943BC3603F4: public key "Max Mustermann <my@email.com>" imported
gpg: Total number processed: 1
gpg:               imported: 1


NEXT STEP: You need to manually check these in:
      git commit -m'NEW ADMIN: herbert@poul.at' .blackbox/pubring.kbx .blackbox/trustdb.gpg .blackbox/blackbox-admins.txt

bash$ git add .
bash$ git commit -m 'initialized blackbox'
[master (root-commit) 65384bd] initialized blackbox
 6 files changed, 6 insertions(+)
 create mode 100644 .blackbox/.gitattributes
 create mode 100644 .blackbox/blackbox-admins.txt
 create mode 100644 .blackbox/blackbox-files.txt
 create mode 100644 .blackbox/pubring.kbx
 create mode 100644 .blackbox/trustdb.gpg
 create mode 100644 .gitignore
bash$
```

# Configure signing keys for android

It's a good practice to keep your secrets in one place instead of scattering
them across the whole project. So I tend to have a `_tools/secrets` folder in
my projects which contain all non-public files.

The easiest way to keep your android release and upload keys secure is to add
a new `build_secrets.gradle.kts` file which is conditionally imported into your
main gradle file if it is available.

(My examples use gradle kotlin dsl. it should only require minor adjustments
for gradle groovy syntax).

**_tools/secrets/android_build_secrets.gradle.kts**

```kotlin
allprojects {
    // requires path relative to the `android` directory.
    extra["secrets.keyAlias"] = "MyKeyAlias"
    extra["secrets.storeFile"] = "../_tools/secrets/android_upload_key.jks"
    extra["secrets.storePassword"] = "MySecretPassword"
}
```

Now copy your upload or signing key to `_tools/secrets/android_upload_key.jks`
and add both to blackbox:

```console
bash$ blackbox_register_new_file _tools/secrets/android_build_secrets.gradle.kts _tools/secrets/android_upload_key.jks 
      ========== PLAINFILE _tools/secrets/android_build_secrets.gradle.kts
      ========== ENCRYPTED _tools/secrets/android_build_secrets.gradle.kts.gpg
      ========== PLAINFILE _tools/secrets/android_upload_key.jks
      ========== ENCRYPTED _tools/secrets/android_upload_key.jks.gpg
Local repo updated.  Please push when ready.
    git push
bash$
```

You should now be left with only `.gpg` files and have the real files 
encrypted and added to your `.gitignore`. To be able to continue using those
files, run `blackbox_postdeploy`

```console
bash$ blackbox_postdeploy
========== EXTRACTED _tools/secrets/android_build_secrets.gradle.kts
========== EXTRACTED _tools/secrets/android_upload_key.jks
========== Decrypting new/changed files: DONE
bash$
```

---

**android/app/build.gradle.kts**

```kotlin
val secretConfig = file("../_tools/secrets/build_secrets.gradle.kts")
if (secretConfig.exists()) {
    apply { from(secretConfig) }
} else {
    println("Warning: Secrets not found, release signing disabled.")
}

// ...

android {
    signingConfigs {
        release {
            keyAlias project.properties["secrets.keyAlias"] ?: ""
            keyPassword project.properties['secrets.storePassword']
            // Provide a dummy default value, null would break builds.
            storeFile file(project.properties['secrets.storeFile'] ?: "invalid")
            storePassword project.properties['secrets.storePassword']
        }
    }
}

```

🎉️ aaaand now you are done. Feel save to commit and push your changes.
Your secrets are save, but you can still conveniently export your signed 
appbundles/apks.

# Secret API Keys for your flutter app

To access "secret" API keys (for example for accessing services like firebase
or analytics) you can use a separate entry point for your flutter app.

So instead of using the default `lib/main.dart` you create your custom
`lib/env/production.dart` with a main method like:

**lib/env/production.dart**
```dart
import '../main.dart';
import './production_secrets.dart';

void main() {
    startApp(ProductionSecrets());
}
```

Now modify your main.dart so we start our app with the provided secrets.
We can then use the [Provider package](https://pub.dev/packages/provider) to 
make it available throughout the app.

**lib/main.dart**

```dart
import 'package:provider/provider.dart';

abstract class Secrets {
    abstract String get firebaseApiKey;
}

void startApp(Secrets secrets) {
    runApp(MyApp(secrets: secrets));
}

class MyApp extends StatelessWidget {
  const MyApp({Key key, this.secrets}) : super(key: key);

  final Secrets secrets;
  @override
  Widget build(BuildContext context) {
    return Provider<Secrets>.value(
      value: secrets,
      child: // ...
    );
  }
}

```

Now simply add your secrets as an implementation of the `Secrets` class
and you are ready to go:

**lib/env/production_secrets.dart**

```dart
class ProductionSecrets implements Secrets {
  @override String get firebaseApiKey => 'D484fDKJE4';
}
```

!!! note "Note"
    To keep things tidy you can put this file into `_tools/secrets/` and
    symlink it into your `lib/env/` folder.

Make sure to register the file with blackbox so it is encrypted correctly.

```console
bash$ blackbox_register_new_file lib/env/production_secrets.dart
========== PLAINFILE lib/env/production_secrets.dart
========== ENCRYPTED lib/env/production_secrets.dart.gpg
========== UPDATING VCS: DONE
Local repo updated.  Please push when ready.
    git push
```

## Update release script to use new secrets.

Now you have to tell `flutter build` (and `flutter run`) to use your new
secrets file. To do this simply pass in the `-t` parameter:

```console
bash$ flutter build appbundle -t lib/env/production.dart
```

If you require it during production you can also set the entrypoint in 
Android Studio:

{{render content=node.embed.figures.entryPoint /}}
