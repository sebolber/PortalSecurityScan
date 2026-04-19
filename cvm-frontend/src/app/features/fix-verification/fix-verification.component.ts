import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  FixVerificationQueryHttpService,
  FixVerificationSummaryView,
  VerificationGrade
} from '../../core/fix-verification/fix-verification.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';

const GRADES: readonly (VerificationGrade | 'ALL')[] = [
  'ALL',
  'A',
  'B',
  'C',
  'UNKNOWN'
];

/**
 * Fix-Verifikations-Uebersicht (Iteration 27e, CVM-65). Iteration 61
 * (CVM-62): Migration von Angular Material auf pure Tailwind-Komponenten.
 */
@Component({
  selector: 'cvm-fix-verification',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    DatePipe,
    AhsBannerComponent,
    EmptyStateComponent,
    CvmIconComponent
  ],
  templateUrl: './fix-verification.component.html',
  styleUrls: ['./fix-verification.component.scss']
})
export class FixVerificationComponent implements OnInit {
  private readonly api = inject(FixVerificationQueryHttpService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly grades = GRADES;

  grade = signal<VerificationGrade | 'ALL'>('ALL');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<readonly FixVerificationSummaryView[]>([]);

  ngOnInit(): void {
    // Iteration 83 (CVM-323): Grade aus queryParams uebernehmen.
    const qp = this.route.snapshot.queryParamMap.get('grade');
    if (qp && (GRADES as readonly string[]).includes(qp)) {
      this.grade.set(qp as VerificationGrade | 'ALL');
    }
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
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { grade: value === 'ALL' ? null : value },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
    void this.laden();
  }

  trackId(_: number, r: FixVerificationSummaryView): string {
    return r.id;
  }
}
