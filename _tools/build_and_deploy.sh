#!/bin/bash

set -xeu

dir="${0%/*}"
basedir="$dir/.."

cd $basedir

DC2F_ENV=production ./dc2f.sh build

#time find ./public -type f -name '*.png' -o -name '*.jpg' | xargs -P 1 -I {} sh -c 'echo $1 ; cwebp -m 6 -af -short -mt -q 75 $1 -o "${1%.*}.webp"' _ {} \;
#
#cat _tools/_htaccess_append >> public/.htaccess

# Disabled: this published to web.sphene.net:public_html/newpage.codeux.design/,
# which is the *staging* host from the 2019 dc2f rewrite (vhost symlinks date
# from Oct 2019, days after the first blog post). codeux.design has never been
# served from it -- that is the docker-host target below, behind Cloudflare.
# It served newpage.codeux.design: a byte-identical public duplicate on an
# unproxied nginx/1.8.1, and the only copy holding hand-placed files
# (hue-poc/, oeamtc/test.txt) that --delete here would have destroyed.
#./_tools/_deploy_web_sphene_net.sh

rsync --progress -a --delete public/ docker-host.tapo.at:dev/web.poul.at/data/sites/newpage.codeux.design/

#echo WARNING WARNING
#echo WARNING WARNING
#echo WARNING WARNING
echo
echo "purge cloudflare cachedisabled"
./_tools/purge_cloudflare_cache.sh

# Disabled: the gh-pages deploy key is rejected by GitHub
# ("Permission denied (publickey)"), so this aborted the script under `set -e`
# *after* the site had already gone live -- making a successful deploy look
# failed. The branch is a vestigial mirror: it has no CNAME, so it never
# served codeux.design, and it had drifted years out of date. Re-add once the
# deploy key is re-established, or drop it for good.
#./_tools/gh-pages-deploy.sh

