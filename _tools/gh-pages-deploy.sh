#!/usr/bin/env bash

# requires a orphaned gh-pages branch.
# see https://help.github.com/en/articles/creating-project-pages-using-the-command-line
# > git checkout --orphan gh-pages
# > git rm -rf .
# > touch README.md && git add README.md && git commit -m 'Initial gh-pages commit.'
# > git push -u origin HEAD
#
# create deploy key for github:
# > ssh-keygen -f github-pages-deploy-key_id_rsa
# add it to: https://github.com/authpass/authpass.app-website/deploy_keys

set -xeu


dir="${0%/*}"
repodir="${dir}/.."
rsa="_tools/secrets/github-pages-deploy-key_id_rsa"

pushd "${repodir}"


REPO_SLUG=${TRAVIS_REPO_SLUG:-hpoul/codeux.design.git}
BUILD_NUMBER=${TRAVIS_BUILD_NUMBER:-manual}
BUILD_WEB_URL=${TRAVIS_BUILD_WEB_URL:-}

tmpdir=tmp

rm -rf "$tmpdir"
mkdir "$tmpdir"

chmod 600 $rsa

GIT_SSH_COMMAND="ssh -v -i $rsa" git clone -b gh-pages git@github.com:${REPO_SLUG} "$tmpdir"

# clean directory...
rm -rf "${tmpdir}/*"
# remove all remaining . files (.htaccess), but keep .git sub directory.
#find "${tmpdir}" -type f -exec rm {} \;

cp -a public/. "${tmpdir}"

pushd "${tmpdir}"

git status
git add .
git commit --author="Travis <ci@travis>" -m "new publish from travis ci ${BUILD_NUMBER} ${BUILD_WEB_URL}"
GIT_SSH_COMMAND="ssh -v -i $rsa" git push

popd


popd
