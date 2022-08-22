#!/usr/bin/env bash
#/* BSD 2-Clause License - see OPAL/LICENSE for details. */

no_cache=""

while [ -n "$1" ]
do
    case "$1" in
        --no-cache) no_cache="$1" ;;
        *) echo "unknown arguments provided"; exit 1 ;;
    esac
shift
done

docker build $no_cache -t opal-apk-parser .

