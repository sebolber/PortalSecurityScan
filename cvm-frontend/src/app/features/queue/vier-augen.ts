import { Severity } from '../../shared/components/severity-badge.component';
import { SEVERITY_RANK, VIER_AUGEN_ZIELWERTE } from './queue.types';

/**
 * Entscheidung, ob eine Freigabe laut Konzept 6.2 eine Zweitfreigabe
 * (Vier-Augen) erfordert.
 *
 * <p>Die Regel ist: ein Downgrade auf {@code NOT_APPLICABLE} oder
 * {@code INFORMATIONAL} darf nicht vom gleichen Benutzer freigegeben
 * werden, der den Vorschlag angelegt hat. Dieser Check wird im Backend
 * erzwungen; das Frontend spiegelt das Signal lediglich visuell.
 *
 * @param zielSeverity aktuell vorgeschlagene Severity.
 * @param originalSeverity urspruengliche (z.B. NVD-) Severity. Fehlt
 *   diese, wird nur der Zielwert geprueft.
 */
export function braucheZweitfreigabe(
  zielSeverity: Severity,
  originalSeverity?: Severity | null
): boolean {
  if (!VIER_AUGEN_ZIELWERTE.includes(zielSeverity)) {
    return false;
  }
  if (!originalSeverity) {
    return true;
  }
  return SEVERITY_RANK[zielSeverity] < SEVERITY_RANK[originalSeverity];
}
