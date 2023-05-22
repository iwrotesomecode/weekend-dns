#!/bin/bash

echo "Building uberjar..."
clj -T:build uber

echo "Nativizing jar..."

native-image --report-unsupported-elements-at-runtime \
    --initialize-at-build-time \
    --diagnostics-mode \
    --no-fallback \
    -jar target/weekend-dns-standalone.jar \
    -H:Name=./dns

echo "Success! Run ./dns domain type"
