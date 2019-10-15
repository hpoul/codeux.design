#!/bin/bash

gradleArgs="-q"
gradleArgs=""
args=""

echo "args: $@"

if test "$1" = "-t" ; then
  shift
  echo "setting gradle args. $@"
  gradleArgs="$gradleArgs -t"
elif test "$1" = "serve" ; then
  echo "Launching serve together with -t (continous build)"
  gradleArgs="$gradleArgs -t"
fi

#if test $# -ge 0 ; then
#    args=--args=\"$@\"
#fi

set -xeu
args=(" ")
args+="$@"

DC2F_ARG0="$0" ./gradlew ${gradleArgs} run --args "$args"


