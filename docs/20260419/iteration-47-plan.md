# Iteration 47 - Queue-Filter oberhalb der Liste

## Ziel

User-Feedback vom 18.04.: der linke Filter-Sidebar macht die Queue-
Tabelle zu schmal. Der Filter soll als horizontaler Balken **oberhalb**
der Liste erscheinen, die Tabelle nutzt die volle Breite.

## Vorgehen

1. Neue Komponente `queue-filter-bar.component.ts` mit dem gleichen
   Verhalten wie die bisherige `queue-filter-sidebar`, aber flex-row-
   Layout.
2. `queue.component.html` umorganisiert:
   - Filter-Balken kommt zwischen Header/Banner und dem Tabellen-
     /Detail-Layout.
   - Die frueher vom Sidebar belegte Breite entfaellt, die Tabelle
     bekommt `flex-1`.
3. Alte `queue-filter-sidebar.component.ts` entfernen (kein
   Backwards-Compat, siehe CLAUDE.md).
4. Spec fuer die neue Komponente.
5. `npx ng build` + `npx ng lint` gruen halten.

## Jira

`CVM-97` - Queue-Filter-Layout.
