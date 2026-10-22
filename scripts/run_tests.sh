#!/bin/sh

# Lance tous les tests (modules model et core) et affiche la couverture du modèle.
# Usage : ./scripts/run_tests.sh
# Appelé aussi avant chaque push par le hook .githooks/pre-push.

set -e


cd "$(dirname "$0")/.."

echo "==> Tous les tests (model + core, JUnit 6) + rapport de couverture (JaCoCo)"
./gradlew --console=plain test

REPORT="model/build/reports/jacoco/test/jacocoTestReport.xml"
if [ ! -f "$REPORT" ]; then
  echo "Rapport de couverture introuvable : $REPORT"
  exit 1
fi


coverage() {
  grep -o "<counter type=\"$1\"[^>]*/>" "$REPORT" | tail -n 1 |
    sed -E 's/.*missed="([0-9]+)" covered="([0-9]+)".*/\1 \2/' |
    awk '{ total = $1 + $2; if (total == 0) print "n/a"; else printf "%.1f %% (%d/%d)\n", 100 * $2 / total, $2, total }'
}

echo ""
echo "==> Tous les tests passent."
echo "    Couverture du modèle, lignes   : $(coverage LINE)"
echo "    Couverture du modèle, branches : $(coverage BRANCH)"
echo "    Rapport détaillé               : model/build/reports/jacoco/test/html/index.html"
