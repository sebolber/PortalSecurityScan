import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import {
  WaiverStatus,
  WaiverView,
  WaiversService
} from '../../core/waivers/waivers.service';

const WAIVER_STATUSSE: readonly WaiverStatus[] = [
  'ACTIVE',
  'EXPIRED',
  'REVOKED'
];

/**
 * Waiver-Basisansicht (Iteration 27c, CVM-63). Ersetzt den
 * Platzhalter aus 27b durch eine Server-gespeiste Liste mit
 * Status-Filter und validUntil-Ablauf-Warner.
 */
@Component({
  selector: 'cvm-waivers',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonToggleModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    AhsBannerComponent,
    EmptyStateComponent,
    DatePipe
  ],
  templateUrl: './waivers.component.html',
  styleUrls: ['./waivers.component.scss']
})
export class WaiversComponent implements OnInit {
  private readonly waivers = inject(WaiversService);

  readonly statusse = WAIVER_STATUSSE;
  readonly columns = [
    'status',
    'assessment',
    'reason',
    'grantedBy',
    'validUntil',
    'age'
  ] as const;

  status = signal<WaiverStatus>('ACTIVE');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<readonly WaiverView[]>([]);

  readonly abgelaufenBald = computed(() =>
    this.rows().filter((w) => this.isBaldAblaufend(w))
  );

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const list = await this.waivers.list(this.status());
      this.rows.set(list);
    } catch {
      this.error.set('Waiver-Liste konnte nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  statusWechseln(value: WaiverStatus): void {
    this.status.set(value);
    void this.laden();
  }

  isBaldAblaufend(waiver: WaiverView): boolean {
    if (waiver.status !== 'ACTIVE' || !waiver.validUntil) {
      return false;
    }
    const diff = new Date(waiver.validUntil).getTime() - Date.now();
    const tage = diff / (1000 * 60 * 60 * 24);
    return tage >= 0 && tage <= 14;
  }

  trackId(_: number, w: WaiverView): string {
    return w.id;
  }
}
