#!/bin/bash

set -xeu

dir="${0%/*}"
basedir="$dir/.."

cd $basedir

DC2F_ENV=production ./dc2f.sh build

./_tools/_deploy_web_sphene_net.sh

./_tools/purge_cloudflare_cache.sh
./_tools/gh-pages-deploy.sh

