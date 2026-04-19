import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import {
  AlertHistoryView,
  AlertsHistoryService
} from '../../core/alerts/alerts-history.service';

/**
 * Alert-Historie (Iteration 27c, CVM-63). Iteration 61 (CVM-62):
 * Migration von Angular Material auf pure Tailwind-Komponenten.
 */
@Component({
  selector: 'cvm-alerts-history',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    AhsBannerComponent,
    EmptyStateComponent,
    CvmIconComponent
  ],
  templateUrl: './alerts-history.component.html',
  styleUrls: ['./alerts-history.component.scss']
})
export class AlertsHistoryComponent implements OnInit {
  private readonly history = inject(AlertsHistoryService);

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
