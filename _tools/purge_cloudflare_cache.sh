#!/bin/bash

# requires jq: https://stedolan.github.io/jq/

set -xeu

dir="${0%/*}"
cd $dir/..

source _tools/secrets/_purgecache_secrets.sh

data=$(cat public/allsites.json | jq '{files: [.pages[].url]}')


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

