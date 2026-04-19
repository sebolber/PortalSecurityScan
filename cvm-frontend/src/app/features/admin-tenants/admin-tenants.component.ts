import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import {
  TenantView,
  TenantsService
} from '../../core/tenants/tenants.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

/**
 * Iteration 56 (CVM-106): Read-only Admin-Liste der Mandanten.
 *
 * <p>Zeigt pro Mandant Key, Name, Active-Flag, Default-Marker und
 * Anlegezeit. Die volle Mandanten-Verwaltung (Anlage, Aktivierung,
 * Keycloak-Mapping) bleibt Admin-SQL bis zur Folge-Iteration.
 */
@Component({
  selector: 'cvm-admin-tenants',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    AhsBannerComponent,
    EmptyStateComponent
  ],
  templateUrl: './admin-tenants.component.html',
  styleUrls: ['./admin-tenants.component.scss']
})
export class AdminTenantsComponent implements OnInit {
  private readonly service = inject(TenantsService);

  readonly columns = ['tenantKey', 'name', 'active', 'default', 'createdAt'] as const;
  readonly rows = signal<readonly TenantView[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.rows.set(await this.service.list());
    } catch {
      this.error.set('Mandanten konnten nicht geladen werden.');
      this.rows.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  trackId(_: number, t: TenantView): string {
    return t.id;
  }
}
