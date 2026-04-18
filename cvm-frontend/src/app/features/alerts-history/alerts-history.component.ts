import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import {
  AlertHistoryView,
  AlertsHistoryService
} from '../../core/alerts/alerts-history.service';

/**
 * Alert-Historie (Iteration 27c, CVM-63). Ersetzt den Placeholder
 * aus 27b durch eine Server-gespeiste Tabelle der letzten
 * Mail-Dispatches.
 */
@Component({
  selector: 'cvm-alerts-history',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    AhsBannerComponent,
    EmptyStateComponent
  ],
  templateUrl: './alerts-history.component.html',
  styleUrls: ['./alerts-history.component.scss']
})
export class AlertsHistoryComponent implements OnInit {
  private readonly history = inject(AlertsHistoryService);

  readonly columns = [
    'dispatchedAt',
    'subject',
    'recipients',
    'triggerKey',
    'state'
  ] as const;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<readonly AlertHistoryView[]>([]);

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.rows.set(await this.history.recent(50));
    } catch {
      this.error.set('Alert-Historie konnte nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  trackId(_: number, a: AlertHistoryView): string {
    return a.id;
  }
}
