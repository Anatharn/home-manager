#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

echo "==> Build du backend Spring (Maven, $(java -version 2>&1 | head -n1))"
(cd "$BACKEND_DIR" && mvn -q clean package -DskipTests)

JAR_FILE=$(find "$BACKEND_DIR/target" -maxdepth 1 -name "*.jar" ! -name "*.original" | head -n 1)

if [ -z "$JAR_FILE" ]; then
  echo "Erreur : aucun jar trouvé dans $BACKEND_DIR/target"
  exit 1
fi

echo "==> Copie du jar dans les ressources Tauri"
mkdir -p "$FRONTEND_DIR/src-tauri/resources"
cp "$JAR_FILE" "$FRONTEND_DIR/src-tauri/resources/backend.jar"

echo "==> Build du frontend Tauri"
(cd "$FRONTEND_DIR" && npm install && npm run tauri build)

echo "==> Terminé. Binaire disponible dans frontend/src-tauri/target/release/"
