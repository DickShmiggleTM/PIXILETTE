#!/usr/bin/env bash
set -euo pipefail

# Uses Java 17 because Android Gradle Plugin requires JDK 17+
if command -v mise >/dev/null 2>&1; then
  exec mise exec java@17.0.2 -- gradle :app:assembleDebug
else
  exec gradle :app:assembleDebug
fi
