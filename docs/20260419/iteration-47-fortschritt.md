# Iteration 47 - Fortschritt

**Thema**: Queue-Filter oberhalb der Tabelle (CVM-97).

## Was gebaut wurde

- Neue Standalone-Komponente
  `queue-filter-bar.component.ts` (Selector `cvm-queue-filter-bar`),
  flex-row-Layout mit `flex-wrap`, identisches Verhalten wie die
  ehemalige Sidebar.
- `queue.component.html` umgebaut: Filter-Balken zwischen
  Banner/Error und dem Layout-Container, Tabelle nimmt den gesamten
  verfuegbaren Platz.
- `queue.component.ts` importiert die neue Komponente.
- Alte `queue-filter-sidebar.component.ts` entfernt (kein
  Backwards-Compat).
- Spec `queue-filter-bar.component.spec.ts` (4 Faelle) deckt
  Eingabe-Trimming, Reset und Severity-Toggle ab.

## Build

- `npx ng build` &rarr; ok (Bundle-Budget-Warnung unveraendert -
  Punkt 52 geplant).
- `npx ng lint` &rarr; All files pass linting.

## Vier Leitfragen (Oberflaeche)

1. *Weiss ein Admin, was zu tun ist?* Ja - die Filter sind mit
   Labels beschriftet, der Balken ist oberhalb der Liste (dominante
   Position).
2. *Ist erkennbar, ob eine Aktion erfolgreich war?* Ja - die
   Queue-Tabelle reagiert sofort auf Filter-Aenderungen; die
   Severity-Chips fuellen sich in der Severity-Farbe.
3. *Sind Daten sichtbar, die im Backend existieren?* Ja - die
   Tabelle hat jetzt die volle Breite und zeigt alle Spalten.
4. *Gibt es einen Weg zurueck/weiter?* Reset-Button entfernt alle
   Filter auf einen Klick.
