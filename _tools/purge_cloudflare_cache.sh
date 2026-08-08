#!/bin/bash

# requires jq: https://stedolan.github.io/jq/

set -xeu

dir="${0%/*}"
cd $dir/..

source _tools/secrets/_purgecache_secrets.sh

# allsites.json lists pages only. sitemap.xml is regenerated on every build and
# was never purged, so Cloudflare served a stale copy for up to max-age (48h)
# and Google kept reading outdated <lastmod> values -- the very signal that
# replaced the retired ping endpoint. Origin is derived from the first page URL
# rather than hardcoded, so this stays reusable across sites.
data=$(cat public/allsites.json | jq '
  [.pages[].url] as $pages
  | ($pages[0] | split("/")[0:3] | join("/")) as $origin
  | {files: ($pages + ["\($origin)/sitemap.xml"])}
')


# we don't want secrets to be echo'd
set +x

result=$(
curl -X POST "https://api.cloudflare.com/client/v4/zones/$CF_ZONE_ID/purge_cache" \
     -H "Authorization: Bearer $CF_API_TOKEN" \
     -H "Content-Type: application/json" \
     --data "$data"
)

set -x

if test "$(echo "$result" | jq '.success')" != "true" ; then
  echo "Unsuccessful purge cache request."
  echo "Response: $result"
  exit 1
fi

echo "SUCCESS 🎉️"

