#!/bin/sh

# Usage : ./scripts/run_tests.sh

set -e


cd "$(dirname "$0")/.."

echo "==> Tests du modèle (JUnit 6) + rapport de couverture (JaCoCo)"
./gradlew --console=plain model:test

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
echo "    Couverture des lignes   : $(coverage LINE)"
echo "    Couverture des branches : $(coverage BRANCH)"
echo "    Rapport détaillé        : model/build/reports/jacoco/test/html/index.html"
