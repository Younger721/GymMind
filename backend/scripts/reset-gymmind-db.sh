#!/usr/bin/env bash
set -euo pipefail

HOST_NAME="localhost"
PORT="3306"
USER_NAME=""
EXECUTE=0
CONFIRMATION=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      HOST_NAME="$2"
      shift 2
      ;;
    --port)
      PORT="$2"
      shift 2
      ;;
    --user)
      USER_NAME="$2"
      shift 2
      ;;
    --execute)
      EXECUTE=1
      shift
      ;;
    --confirmation)
      CONFIRMATION="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

if [[ -z "$USER_NAME" ]]; then
  echo "--user is required" >&2
  exit 2
fi

if [[ "$EXECUTE" -eq 0 ]]; then
  cat <<'EOF'
Dry-run only. No database was changed.
Target: gymmind
DROP DATABASE IF EXISTS gymmind;
CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
To execute, pass --execute --confirmation "DROP gymmind".
EOF
  exit 0
fi

if [[ "$CONFIRMATION" != "DROP gymmind" ]]; then
  echo "Refusing to reset the database. Exact confirmation required: DROP gymmind" >&2
  exit 2
fi

command -v mysql >/dev/null 2>&1 || {
  echo "mysql CLI was not found on PATH" >&2
  exit 127
}

echo "Resetting only the exact database target: gymmind"
mysql --host="$HOST_NAME" --port="$PORT" --user="$USER_NAME" --password \
  --batch --skip-column-names --execute='DROP DATABASE IF EXISTS gymmind;'
mysql --host="$HOST_NAME" --port="$PORT" --user="$USER_NAME" --password \
  --batch --skip-column-names --execute='CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;'
echo "Database gymmind was recreated."
