#!/usr/bin/env sh
set -eu

TARGET="${1:-$HOME/Desktop/cursor}"
APPDIR="$TARGET/agenda-quimioterapia"
DIST="target/agenda-quimioterapia-dist"
JAR="agenda-quimioterapia-1.7.0.jar"

MVN_CMD="./mvnw"
if [ ! -x "$MVN_CMD" ]; then
  MVN_CMD="mvn"
fi

echo "Compilando aplicacion..."
$MVN_CMD -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$DIST/lib"

mkdir -p "$APPDIR/lib"
cp "target/$JAR" "$APPDIR/$JAR"
cp -R "$DIST/lib/." "$APPDIR/lib/"
cp "scripts/run-agenda.sh" "$APPDIR/run-agenda.sh"
chmod +x "$APPDIR/run-agenda.sh"

echo
echo "Despliegue completado en:"
echo "$APPDIR"
echo "Ejecute ./run-agenda.sh para iniciar la agenda."
