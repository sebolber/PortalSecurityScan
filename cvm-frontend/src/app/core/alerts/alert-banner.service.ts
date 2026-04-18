import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

/** Antwortform des Backend-Endpunkts {@code GET /api/v1/alerts/banner}. */
export interface AlertBannerStatus {
  readonly visible: boolean;
  readonly count: number;
  readonly t2Minutes: number;
}

/**
 * Liest den Banner-Status aus dem Backend und stellt ihn als Signal
 * bereit. Polling triggert die Shell selbst (alle 60&nbsp;s).
 */
@Injectable({ providedIn: 'root' })
export class AlertBannerService {
  private readonly api = inject(ApiClient);
  private readonly statusSig = signal<AlertBannerStatus | null>(null);

  readonly status = this.statusSig.asReadonly();

  async refresh(): Promise<void> {
    try {
      const status = await firstValueFrom(
        this.api.get<AlertBannerStatus>('/api/v1/alerts/banner')
      );
      this.statusSig.set(status);
    } catch {
      // Backend nicht erreichbar -> Banner aus.
      this.statusSig.set(null);
    }
  }
}
