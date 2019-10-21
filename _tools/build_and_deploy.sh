#!/bin/bash

set -xeu

dir="${0%/*}"
basedir="$dir/.."

cd $basedir

DC2F_ENV=production ./dc2f.sh build

#time find ./public -type f -name '*.png' -o -name '*.jpg' | xargs -P 1 -I {} sh -c 'echo $1 ; cwebp -m 6 -af -short -mt -q 75 $1 -o "${1%.*}.webp"' _ {} \;
#
#cat _tools/_htaccess_append >> public/.htaccess

./_tools/_deploy_web_sphene_net.sh

rsync --progress -a --delete public/ docker-host.tapo.at:dev/web.poul.at/data/sites/newpage.codeux.design/

#echo WARNING WARNING
#echo WARNING WARNING
#echo WARNING WARNING
echo
echo "purge cloudflare cachedisabled"
./_tools/purge_cloudflare_cache.sh

./_tools/gh-pages-deploy.sh

