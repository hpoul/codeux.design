For every flutter mobile app there are certain secrets which have to be
integrated into the app. These might include API Keys or private keys
for services like Firebase, Analytics, etc. Or provisioning profiles for 
iOS.

# Blackbox Overview

It is generally not a good idea to add plaintext API keys, passwords, etc.
to your git repository. But it is usually very convenient. This is where
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

# Secret API Keys for your flutter app

