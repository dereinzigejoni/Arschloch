#!/bin/bash


# Stelle sicher, dass es ein Git-Repository ist
if [ ! -d ".git" ]; then
  echo "Kein Git-Repository gefunden!"
  exit 1
fi

# Alle Änderungen hinzufügen
git add .

# Commit-Nachricht mit aktuellem Datum und Uhrzeit
COMMIT_MESSAGE="Auto-Commit am $(date +'%Y-%m-%d %H:%M:%S')"

# Commit durchführen
git commit -m "$COMMIT_MESSAGE"

# Änderungen pushen
git push

echo "Änderungen erfolgreich gespeichert und hochgeladen!"

