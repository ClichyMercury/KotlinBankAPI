#!/usr/bin/env bash
# start.sh — démarre la stack FinSim (Docker + API) en une commande (macOS/Linux).
# Usage : ./start.sh   (rends-le exécutable une fois : chmod +x start.sh)
set -euo pipefail

# Se placer dans le dossier du script (racine du projet)
cd "$(dirname "$0")"

# 1. JAVA_HOME — JDK 17 si pas déjà défini
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  if [ -x /usr/libexec/java_home ]; then
    # macOS
    JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    export JAVA_HOME
  fi
fi
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "JDK 17 introuvable." >&2
  echo "macOS  : brew install --cask temurin@17" >&2
  echo "Linux  : installe un JDK 17 puis exporte JAVA_HOME" >&2
  exit 1
fi
echo "JAVA_HOME = $JAVA_HOME"

# 2. Vérifier que Docker répond
if ! docker info >/dev/null 2>&1; then
  echo "Docker ne répond pas. Lance Docker Desktop puis réessaie." >&2
  exit 1
fi

# 3. Démarrer Postgres + Redis
echo "Démarrage de Postgres + Redis..."
docker compose up -d

# 4. Attendre que les containers soient healthy (max ~60s)
echo "Attente des containers healthy..."
deadline=$(( $(date +%s) + 60 ))
while true; do
  states="$(docker compose ps --format '{{.Health}}' 2>/dev/null || true)"
  if [ -n "$states" ] && ! printf '%s\n' "$states" | grep -qv '^healthy$'; then
    break
  fi
  if [ "$(date +%s)" -gt "$deadline" ]; then
    echo "Timeout : containers pas healthy. Vérifie 'docker compose ps'." >&2
    exit 1
  fi
  sleep 2
done
echo "Postgres + Redis healthy."

# 5. Lancer l'API (foreground, Ctrl+C pour stopper)
echo "Lancement de l'API sur http://localhost:8080 (Ctrl+C pour arrêter)..."
./gradlew run
