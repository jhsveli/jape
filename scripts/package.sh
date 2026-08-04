#!/usr/bin/env bash
#
# Builds a distributable Jape app image with jlink + jpackage.
#
# Produces: target/dist/Jape/  (Jape.exe launcher + app jar + trimmed runtime)
#
# Prerequisites:
#   - A full JDK.  Resolved automatically: $JAVA_HOME if it is a full JDK,
#     otherwise derived from `java` on PATH, otherwise scanned from the
#     common JDK install locations (.jdks, sdkman, Program Files, /usr/lib/jvm,
#     macOS).  jlink/jpackage/jmods must be present.
#   - mvn on PATH (or set MVN to a maven executable)
#   - run from the repository root, or anywhere (script locates the root)
#
# Optional overrides: APP_NAME, APP_VERSION, ICON
set -euo pipefail

cd "$(dirname "$0")/.."   # repo root

# --- Resolve the JDK -------------------------------------------------------
find_jdk() {
    local home dir candidate candidates=""

    # 1. Explicit JAVA_HOME (must be a full JDK)
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jlink" ] \
        && [ -x "$JAVA_HOME/bin/jpackage" ] && [ -d "$JAVA_HOME/jmods" ]; then
        return 0
    fi

    # 2. Derive from the java on PATH
    if command -v java >/dev/null 2>&1; then
        home="$(java -XshowSettings:properties -version 2>&1 \
                | sed -n 's/^[[:space:]]*java\.home = //p' | head -1)"
        home="${home//\\//}"   # windows paths -> forward slashes for bash
        if [ -n "$home" ] && [ -x "$home/bin/jlink" ] \
            && [ -x "$home/bin/jpackage" ] && [ -d "$home/jmods" ]; then
            JAVA_HOME="$home"
            return 0
        fi
    fi

    # 3. Scan common JDK install locations, newest first
    for dir in "$HOME/.jdks" "$HOME/.sdkman/candidates/java" \
               "/c/Program Files/Java" "/c/Program Files/Eclipse Adoptium" \
               "/c/Program Files/Microsoft" "/usr/lib/jvm" \
               "/Library/Java/JavaVirtualMachines"; do
        [ -d "$dir" ] || continue
        for candidate in "$dir"/*/; do
            [ -d "$candidate" ] || continue
            home="${candidate%/}"
            if [ -d "$home/Contents/Home" ]; then   # macOS layout
                home="$home/Contents/Home"
            fi
            if [ -x "$home/bin/jlink" ] && [ -x "$home/bin/jpackage" ] \
                && [ -d "$home/jmods" ]; then
                candidates="$candidates
$home"
            fi
        done
    done
    if [ -n "$candidates" ]; then
        JAVA_HOME="$(printf '%s\n' "$candidates" | sed '/^$/d' | sort -Vr | head -1)"
        return 0
    fi
    return 1
}

if ! find_jdk; then
    echo "error: could not locate a full JDK. Set JAVA_HOME to a JDK (with" >&2
    echo "bin/jlink, bin/jpackage and jmods), or install one in a standard" >&2
    echo "location such as ~/.jdks or Program Files." >&2
    exit 1
fi
# Export so child processes (mvn, jdeps, jlink, jpackage) all use it
export JAVA_HOME
echo "    using JDK: $JAVA_HOME"

APP_NAME="${APP_NAME:-Jape}"
APP_VERSION="${APP_VERSION:-1.0.0}"
JAR_NAME="jape-1.0-SNAPSHOT.jar"
JAR="target/$JAR_NAME"
MVN="${MVN:-mvn}"

echo "==> Building jar ($MVN clean package)"
"$MVN" -q clean package

echo "==> Determining required JDK modules"
MODULES="$("$JAVA_HOME/bin/jdeps" --print-module-deps --ignore-missing-deps "$JAR" 2>/dev/null || true)"
if [ -z "$MODULES" ]; then
    # Fallback for a Swing app: java.desktop covers Swing/AWT, java.logging
    # is used by some libraries.
    MODULES="java.base,java.desktop,java.logging"
fi
echo "    modules: $MODULES"

echo "==> Building trimmed runtime (jlink)"
"$JAVA_HOME/bin/jlink" \
    --module-path "$JAVA_HOME/jmods" \
    --add-modules "$MODULES" \
    --strip-debug --no-header-files --no-man-pages --compress zip-6 \
    --output target/runtime

echo "==> Packaging app image (jpackage)"
rm -rf target/package-input
mkdir -p target/package-input
cp "$JAR" target/package-input/
JPACKAGE_ARGS=(
    --type app-image
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --input target/package-input
    --main-jar "$JAR_NAME"
    --main-class jts.JapeGui
    --runtime-image target/runtime
    --dest target/dist
)
if [ -n "${ICON:-}" ]; then
    JPACKAGE_ARGS+=(--icon "$ICON")
fi
"$JAVA_HOME/bin/jpackage" "${JPACKAGE_ARGS[@]}"

echo "==> Done: target/dist/$APP_NAME/ ($(du -sh "target/dist/$APP_NAME" | cut -f1))"
