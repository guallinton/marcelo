#!/usr/bin/env sh
set -eu

APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$APP_DIR"

if ! command -v java >/dev/null 2>&1; then
  echo "No se encontro Java. Instale Java 17 o superior y vuelva a ejecutar este archivo." >&2
  exit 1
fi

exec java -cp "agenda-quimioterapia-1.5.0.jar:lib/*" com.oncologia.agenda.AppLauncher
