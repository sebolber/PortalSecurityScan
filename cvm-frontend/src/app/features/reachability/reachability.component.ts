import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import {
  ReachabilityQueryService,
  ReachabilitySummaryView
} from '../../core/reachability/reachability.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

/**
 * Reachability-Uebersicht (Iteration 27e, CVM-65). Ersetzt den
 * Platzhalter aus 27d durch eine Server-gespeiste Liste der
 * letzten Reachability-AiSuggestions.
 */
@Component({
  selector: 'cvm-reachability',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    DecimalPipe,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    AhsBannerComponent,
    EmptyStateComponent
  ],
  templateUrl: './reachability.component.html',
  styleUrls: ['./reachability.component.scss']
})
export class ReachabilityComponent implements OnInit {
  private readonly api = inject(ReachabilityQueryService);

  readonly columns = [
    'createdAt',
    'severity',
    'status',
    'rationale',
    'confidence',
    'finding'
  ] as const;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<readonly ReachabilitySummaryView[]>([]);

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.rows.set(await this.api.list(50));
    } catch {
      this.error.set('Reachability-Analysen konnten nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  trackId(_: number, r: ReachabilitySummaryView): string {
    return r.id;
  }
}
