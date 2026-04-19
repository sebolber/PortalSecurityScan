import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  TenantView,
  TenantsService
} from '../../core/tenants/tenants.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';
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
    AhsBannerComponent,
    CvmIconComponent,
    EmptyStateComponent
  ],
  templateUrl: './admin-tenants.component.html',
  styleUrls: ['./admin-tenants.component.scss']
})
export class AdminTenantsComponent implements OnInit {
  private readonly service = inject(TenantsService);
  private readonly toast = inject(CvmToastService);

  readonly rows = signal<readonly TenantView[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // Iteration 59 (CVM-109): Anlage-Formular.
  readonly formOpen = signal(false);
  readonly form = signal<TenantForm>(initialForm());
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);

  // Iteration 62 (CVM-62): Default-Mandant-Pending-Marker (Row-Id).
  readonly defaultPending = signal<string | null>(null);

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
      this.toast.success('Mandant "' + saved.tenantKey + '" angelegt.', 4000);
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

  /**
   * Iteration 62 (CVM-62): Mandanten als Default setzen. Nur fuer aktive
   * Mandanten. Der bisherige Default wird im Backend zurueckgesetzt.
   */
  async alsDefaultSetzen(t: TenantView): Promise<void> {
    if (!t.active) {
      this.toast.warning('Nur aktive Mandanten koennen Default sein.');
      return;
    }
    if (t.defaultTenant) {
      return;
    }
    this.defaultPending.set(t.id);
    try {
      const updated = await this.service.setDefault(t.id);
      this.toast.success(
        '"' + updated.tenantKey + '" ist jetzt Default-Mandant.',
        3000
      );
      await this.laden();
    } catch {
      this.toast.error('Default-Setzen fehlgeschlagen.');
    } finally {
      this.defaultPending.set(null);
    }
  }

  /** Iteration 60 (CVM-110): Mandanten-Aktivitaet umschalten. */
  async toggleActive(t: TenantView): Promise<void> {
    if (t.defaultTenant && t.active) {
      this.toast.warning('Default-Mandant kann nicht deaktiviert werden.');
      return;
    }
    try {
      const updated = await this.service.setActive(t.id, !t.active);
      this.toast.success(
        'Mandant "' + updated.tenantKey + '" '
          + (updated.active ? 'aktiviert' : 'deaktiviert') + '.',
        3000
      );
      await this.laden();
    } catch {
      this.toast.error('Toggle fehlgeschlagen.');
    }
  }
}
