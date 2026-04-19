import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  CvePageResponse,
  CveSeverity,
  CvesService,
  CveView
} from '../../core/cves/cves.service';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

const SEVERITIES: readonly CveSeverity[] = [
  'CRITICAL',
  'HIGH',
  'MEDIUM',
  'LOW',
  'INFORMATIONAL'
];

@Component({
  selector: 'cvm-cves',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CvmIconComponent,
    SeverityBadgeComponent,
    EmptyStateComponent
  ],
  templateUrl: './cves.component.html',
  styleUrls: ['./cves.component.scss']
})
export class CvesComponent implements OnInit {
  private readonly cves = inject(CvesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly severities = SEVERITIES;

  searchText = '';
  severityFilter: CveSeverity | null = null;
  onlyKev = false;

  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly page = signal<CvePageResponse | null>(null);
  pageIndex = 0;
  readonly pageSize = 25;

  ngOnInit(): void {
    // Iteration 83 (CVM-323): Filter-URL-Persistenz.
    const params = this.route.snapshot.queryParamMap;
    this.searchText = params.get('q') ?? '';
    const sev = params.get('severity');
    this.severityFilter = (SEVERITIES as readonly string[]).includes(sev ?? '')
      ? (sev as CveSeverity)
      : null;
    this.onlyKev = params.get('kev') === 'true';
    const pageNum = Number(params.get('page') ?? '0');
    this.pageIndex = Number.isFinite(pageNum) && pageNum >= 0 ? pageNum : 0;
    void this.laden();
  }

  /**
   * Iteration 83 (CVM-323): Filter-State in die URL spiegeln.
   */
  private syncUrl(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.searchText.trim() === '' ? null : this.searchText.trim(),
        severity: this.severityFilter,
        kev: this.onlyKev ? 'true' : null,
        page: this.pageIndex > 0 ? this.pageIndex : null
      },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const page = await this.cves.findPage({
        q: this.searchText,
        severity: this.severityFilter,
        kev: this.onlyKev,
        page: this.pageIndex,
        size: this.pageSize
      });
      this.page.set(page);
    } catch {
      this.error.set('CVEs konnten nicht geladen werden.');
      this.page.set(null);
    } finally {
      this.loading.set(false);
    }
  }

  sucheAusloesen(): void {
    this.pageIndex = 0;
    this.syncUrl();
    void this.laden();
  }

  resetFilter(): void {
    this.searchText = '';
    this.severityFilter = null;
    this.onlyKev = false;
    this.sucheAusloesen();
  }

  nextPage(): void {
    const p = this.page();
    if (!p) return;
    if ((this.pageIndex + 1) * this.pageSize >= p.totalElements) return;
    this.pageIndex++;
    this.syncUrl();
    void this.laden();
  }

  prevPage(): void {
    if (this.pageIndex === 0) return;
    this.pageIndex--;
    this.syncUrl();
    void this.laden();
  }

  setSeverity(s: CveSeverity | null): void {
    this.severityFilter = s;
    this.sucheAusloesen();
  }

  schwereVonCvss(score: number | null): CveSeverity {
    if (score === null || score === undefined) return 'INFORMATIONAL';
    if (score >= 9) return 'CRITICAL';
    if (score >= 7) return 'HIGH';
    if (score >= 4) return 'MEDIUM';
    if (score > 0) return 'LOW';
    return 'INFORMATIONAL';
  }

  trackCve(_: number, c: CveView): string {
    return c.id;
  }
}
