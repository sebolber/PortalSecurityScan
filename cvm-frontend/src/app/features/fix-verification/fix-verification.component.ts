import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import {
  FixVerificationQueryHttpService,
  FixVerificationSummaryView,
  VerificationGrade
} from '../../core/fix-verification/fix-verification.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

const GRADES: readonly (VerificationGrade | 'ALL')[] = [
  'ALL',
  'A',
  'B',
  'C',
  'UNKNOWN'
];

/**
 * Fix-Verifikations-Uebersicht (Iteration 27e, CVM-65). Ersetzt den
 * Platzhalter aus 27d durch eine Server-gespeiste Liste der letzten
 * Mitigation-Plaene mit ihrem Quality-Grade A/B/C/UNKNOWN.
 */
@Component({
  selector: 'cvm-fix-verification',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    MatButtonToggleModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    AhsBannerComponent,
    EmptyStateComponent
  ],
  templateUrl: './fix-verification.component.html',
  styleUrls: ['./fix-verification.component.scss']
})
export class FixVerificationComponent implements OnInit {
  private readonly api = inject(FixVerificationQueryHttpService);

  readonly grades = GRADES;
  readonly columns = [
    'createdAt',
    'strategy',
    'status',
    'target',
    'grade',
    'verifiedAt'
  ] as const;

  grade = signal<VerificationGrade | 'ALL'>('ALL');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<readonly FixVerificationSummaryView[]>([]);

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const filter =
        this.grade() === 'ALL' ? null : (this.grade() as VerificationGrade);
      this.rows.set(await this.api.list(filter, 50));
    } catch {
      this.error.set('Fix-Verifikationen konnten nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  gradeWechseln(value: VerificationGrade | 'ALL'): void {
    this.grade.set(value);
    void this.laden();
  }

  trackId(_: number, r: FixVerificationSummaryView): string {
    return r.id;
  }
}
