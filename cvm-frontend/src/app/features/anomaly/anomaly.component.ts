import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AnomalyService,
  AnomalyView
} from '../../core/anomaly/anomaly.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';

const FENSTER: readonly { key: number; label: string }[] = [
  { key: 24, label: '24 h' },
  { key: 72, label: '3 Tage' },
  { key: 168, label: '7 Tage' }
];

/**
 * Anomalie-Board (Iteration 27d, CVM-64). Iteration 61 (CVM-62):
 * Migration von Angular Material auf pure Tailwind-Komponenten.
 */
@Component({
  selector: 'cvm-anomaly',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    AhsBannerComponent,
    EmptyStateComponent,
    CvmIconComponent
  ],
  templateUrl: './anomaly.component.html',
  styleUrls: ['./anomaly.component.scss']
})
export class AnomalyComponent implements OnInit {
  private readonly service = inject(AnomalyService);

  readonly fenster = FENSTER;

  hours = signal<number>(24);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<readonly AnomalyView[]>([]);

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.rows.set(await this.service.list(this.hours()));
    } catch {
      this.error.set('Anomalien konnten nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  fensterWechseln(value: number): void {
    this.hours.set(value);
    void this.laden();
  }

  trackId(_: number, a: AnomalyView): string {
    return a.id;
  }
}
