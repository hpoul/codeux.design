#!/bin/bash

# requires jq: https://stedolan.github.io/jq/

set -xeu

dir="${0%/*}"
cd $dir/..

source _tools/secrets/_purgecache_secrets.sh

# allsites.json lists pages only, but the build also emits feeds. Both were
# regenerated on every build and never purged, so Cloudflare served stale copies
# for up to max-age (48h): Google kept reading outdated <lastmod> values -- the
# very signal that replaced the retired ping endpoint -- and RSS readers kept
# getting a feed that predated the newest article. Origin is derived from the
# first page URL rather than hardcoded, so this stays reusable across sites.
data=$(cat public/allsites.json | jq '
  [.pages[].url] as $pages
  | ($pages[0] | split("/")[0:3] | join("/")) as $origin
  | {files: ($pages + [
      "\($origin)/sitemap.xml",
      "\($origin)/articles/feed-rss.xml"
    ])}
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

