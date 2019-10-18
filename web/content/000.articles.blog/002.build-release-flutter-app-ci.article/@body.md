One of the first things when starting a new flutter project for me is making
sure it is easy to actually deliver it. Especially for open source projects
this is has some challenges on a public CI/CD like travis or cirrus ci.

# Overview

The things to consider when building a typical flutter app for android.

* Blackbox: Manage Secrets which might include:
    * Upload or signing keys.
    * Secret API Keys for services used in the app
      (like Google Analytics, etc.).
    * Provisioning Profiles or similar.
* Install all app dependencies on the CI.
    * Android SDK
    * Flutter
    * Blackbox
* [Synchronize build numbers](https://github.com/hpoul/git-buildnumber) between build servers and branches.


# Blackbox: Managing Secrets

[Blackbox](https://github.com/StackExchange/blackbox) are a collection of scripts based on GPG to store secrets inside
public git repositories.

[Check out the deep dive in how to use blackbox for your flutter app](../manage-secrets-flutter-project).

