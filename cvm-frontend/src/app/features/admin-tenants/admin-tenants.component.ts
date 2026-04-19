import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import {
  TenantView,
  TenantsService
} from '../../core/tenants/tenants.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

interface TenantForm {
  tenantKey: string;
  name: string;
  active: boolean;
}

function initialForm(): TenantForm {
  return { tenantKey: '', name: '', active: true };
}

/**
 * Iteration 56 (CVM-106): Read-only Admin-Liste der Mandanten.
 * Iteration 59 (CVM-109): Anlage-Formular.
 */
@Component({
  selector: 'cvm-admin-tenants',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    MatTableModule,
    AhsBannerComponent,
    EmptyStateComponent
  ],
  templateUrl: './admin-tenants.component.html',
  styleUrls: ['./admin-tenants.component.scss']
})
export class AdminTenantsComponent implements OnInit {
  private readonly service = inject(TenantsService);
  private readonly snackBar = inject(MatSnackBar);

  readonly columns = ['tenantKey', 'name', 'active', 'default', 'createdAt'] as const;
  readonly rows = signal<readonly TenantView[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // Iteration 59 (CVM-109): Anlage-Formular.
  readonly formOpen = signal(false);
  readonly form = signal<TenantForm>(initialForm());
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);

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

  formularUmschalten(): void {
    this.formOpen.update((v) => !v);
    this.saveError.set(null);
    if (!this.formOpen()) {
      this.form.set(initialForm());
    }
  }

  update<K extends keyof TenantForm>(key: K, value: TenantForm[K]): void {
    this.form.update((f) => ({ ...f, [key]: value }));
  }

  async anlegen(): Promise<void> {
    const f = this.form();
    if (!f.tenantKey.trim() || !f.name.trim()) {
      this.saveError.set('tenantKey und name sind Pflichtfelder.');
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    try {
      const saved = await this.service.create({
        tenantKey: f.tenantKey.trim(),
        name: f.name.trim(),
        active: f.active
      });
      this.snackBar.open(
        'Mandant "' + saved.tenantKey + '" angelegt.',
        'OK',
        { duration: 4000 }
      );
      this.form.set(initialForm());
      this.formOpen.set(false);
      await this.laden();
    } catch (err) {
      this.saveError.set(
        err instanceof Error && err.message
          ? err.message
          : 'Anlage fehlgeschlagen (doppelter key?).'
      );
    } finally {
      this.saving.set(false);
    }
  }

  trackId(_: number, t: TenantView): string {
    return t.id;
  }
}
