import { Severity } from '../../shared/components/severity-badge.component';

/**
 * Fachliche Status eines Assessments (Spiegel zum Backend-Enum
 * {@code AssessmentStatus}). Nur Werte auflisten, die das Frontend
 * aktuell auswerten muss.
 */
export type AssessmentStatus =
  | 'PROPOSED'
  | 'APPROVED'
  | 'REJECTED'
  | 'NEEDS_REVIEW'
  | 'SUPERSEDED'
  | 'EXPIRED';

/** Quelle eines Bewertungs-Vorschlags (Spiegel zu {@code ProposalSource}). */
export type ProposalSource = 'REUSE' | 'RULE' | 'AI' | 'MANUAL';

/**
 * Queue-Eintrag, wie ihn {@code GET /api/v1/findings} liefert
 * ({@code AssessmentResponse}).
 */
export interface QueueEntry {
  readonly id: string;
  readonly findingId: string;
  readonly cveId: string;
  readonly cveKey: string;
  readonly severity: Severity;
  readonly status: AssessmentStatus;
  readonly source: ProposalSource;
  readonly rationale: string | null;
  readonly decidedBy: string | null;
  readonly version: number;
  readonly createdAt: string;
}

/** Filter-Kombination fuer die Queue-Abfrage. */
export interface QueueFilter {
  readonly status?: AssessmentStatus;
  readonly productVersionId?: string;
  readonly environmentId?: string;
  readonly source?: ProposalSource;
  readonly severityIn?: readonly Severity[];
}

/** Eingabe fuer {@code POST /api/v1/assessments/{id}/approve}. */
export interface ApproveCommand {
  readonly approverId: string;
  readonly strategy?: string;
  readonly targetVersion?: string;
  readonly plannedFor?: string;
  readonly mitigationNotes?: string;
}

/** Eingabe fuer {@code POST /api/v1/assessments/{id}/reject}. */
export interface RejectCommand {
  readonly approverId: string;
  readonly comment: string;
}

/** Severity-Reihenfolge fuer Sortierung und Vier-Augen-Heuristik. */
export const SEVERITY_RANK: Record<Severity, number> = {
  CRITICAL: 5,
  HIGH: 4,
  MEDIUM: 3,
  LOW: 2,
  INFORMATIONAL: 1,
  NOT_APPLICABLE: 0
};

/**
 * Zielwerte, die laut Konzept 6.2 eine Zweitfreigabe ausloesen.
 * Deckt sich mit {@code AssessmentWriteService.VIER_AUGEN_DOWNGRADE}.
 */
export const VIER_AUGEN_ZIELWERTE: readonly Severity[] = [
  'NOT_APPLICABLE',
  'INFORMATIONAL'
];

/**
 * Reihenfolge fuer das Rendering der Severity-Filter.
 */
export const SEVERITY_REIHENFOLGE: readonly Severity[] = [
  'CRITICAL',
  'HIGH',
  'MEDIUM',
  'LOW',
  'INFORMATIONAL',
  'NOT_APPLICABLE'
];
