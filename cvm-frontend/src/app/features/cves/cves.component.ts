import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  CvePageResponse,
  CveSeverity,
  CvesService,
  CveView
} from '../../core/cves/cves.service';

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
    MatButtonModule,
    MatButtonToggleModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    MatTableModule,
    MatPaginatorModule,
    MatTooltipModule
  ],
  templateUrl: './cves.component.html',
  styleUrls: ['./cves.component.scss']
})
export class CvesComponent implements OnInit {
  private readonly cves = inject(CvesService);

  readonly severities = SEVERITIES;
  readonly columns = [
    'cveId',
    'severity',
    'cvss',
    'kev',
    'epss',
    'publishedAt',
    'summary'
  ] as const;

  searchText = '';
  severityFilter: CveSeverity | null = null;
  onlyKev = false;

  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly page = signal<CvePageResponse | null>(null);
  pageIndex = 0;
  readonly pageSize = 25;

  ngOnInit(): void {
    void this.laden();
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
    void this.laden();
  }

  resetFilter(): void {
    this.searchText = '';
    this.severityFilter = null;
    this.onlyKev = false;
    this.sucheAusloesen();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    void this.laden();
  }

  schwereVonCvss(score: number | null): CveSeverity {
    if (score === null || score === undefined) {
      return 'INFORMATIONAL';
    }
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
