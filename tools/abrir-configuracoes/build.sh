#!/usr/bin/env bash
# Compila o "Abrir Configurações" sem Gradle: aapt2 + javac + d8 + apksigner.
#
# Uso:
#   ./build.sh            gera build/abrir-configuracoes.apk
#   ./build.sh install    gera e instala no aparelho conectado ao adb
#
# Mesmos requisitos e mesma assinatura do build.sh do Cineluz (pasta raiz).
set -euo pipefail
cd "$(dirname "$0")"

SDK=""
for candidate in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    if [ -n "$candidate" ] && [ -d "$candidate/build-tools" ]; then SDK="$candidate"; break; fi
done
[ -n "$SDK" ] || { echo "Android SDK não encontrado (defina ANDROID_HOME)." >&2; exit 1; }

BT="$SDK/build-tools/$(ls "$SDK/build-tools" | sort -V | tail -1)"
ANDROID_JAR="$SDK/platforms/android-30/android.jar"
[ -f "$ANDROID_JAR" ] || { echo "Falta platforms;android-30 no SDK ($SDK)." >&2; exit 1; }

if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ]; then
    export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home
fi
[ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"

OUT="build/abrir-configuracoes.apk"
rm -rf build && mkdir -p build/classes build/dex

"$BT/aapt2" link -o build/unsigned.apk -I "$ANDROID_JAR" --manifest AndroidManifest.xml
javac --release 11 -Xlint:-options -classpath "$ANDROID_JAR" -d build/classes $(find src -name '*.java')
"$BT/d8" --min-api 21 --lib "$ANDROID_JAR" --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk

if [ -n "${CINELUZ_KEYSTORE:-}" ]; then
    KS="$CINELUZ_KEYSTORE"
    KS_PASS="${CINELUZ_KEYSTORE_PASSWORD:?defina CINELUZ_KEYSTORE_PASSWORD}"
    ALIAS="${CINELUZ_KEY_ALIAS:?defina CINELUZ_KEY_ALIAS}"
    KEY_PASS="${CINELUZ_KEY_PASSWORD:-$KS_PASS}"
else
    KS="$HOME/.cineluz/debug.keystore"
    KS_PASS="android"
    ALIAS="debug"
    KEY_PASS="android"
    if [ ! -f "$KS" ]; then
        mkdir -p "$(dirname "$KS")"
        keytool -genkeypair -keystore "$KS" -alias "$ALIAS" -storepass "$KS_PASS" -keypass "$KEY_PASS" \
            -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Cineluz Debug" >/dev/null 2>&1
    fi
    echo "Assinando com a chave de teste ($KS)."
fi

"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --ks-key-alias "$ALIAS" \
    --key-pass "pass:$KEY_PASS" --out "$OUT" build/aligned.apk 2> >(grep -v "^WARNING" >&2)
echo "APK: $OUT ($(du -h "$OUT" | cut -f1))"

if [ "${1:-}" = "install" ]; then
    adb install -r "$OUT"
fi
