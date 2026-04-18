import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from '../api/api-client.service';
import { BrandingConfig } from './branding';

export interface BrandingUpdateCommand {
  primaryColor: string;
  primaryContrastColor: string;
  accentColor?: string | null;
  fontFamilyName: string;
  fontFamilyMonoName?: string | null;
  appTitle?: string | null;
  logoUrl?: string | null;
  logoAltText?: string | null;
  faviconUrl?: string | null;
  fontFamilyHref?: string | null;
  expectedVersion: number;
}

export type BrandingAssetKind = 'LOGO' | 'FAVICON' | 'FONT';

export interface BrandingAssetResponse {
  readonly id: string;
  readonly kind: string;
  readonly contentType: string;
  readonly sizeBytes: number;
  readonly sha256: string;
  readonly url: string;
}

/**
 * UI-Fix MEDIUM-4 (UI-Exploration 20260418): Iteration 31 hat
 * {@code GET /admin/theme/history} und {@code POST /admin/theme/rollback/{version}}
 * ausgeliefert, das Frontend nutzt sie noch nicht. Diese Projektion
 * spiegelt den Response des Backends 1:1.
 */
export interface BrandingHistoryEntry {
  readonly version: number;
  readonly primaryColor: string;
  readonly primaryContrastColor: string;
  readonly accentColor: string | null;
  readonly fontFamilyName: string;
  readonly fontFamilyMonoName: string | null;
  readonly appTitle: string | null;
  readonly logoUrl: string | null;
  readonly logoAltText: string | null;
  readonly faviconUrl: string | null;
  readonly fontFamilyHref: string | null;
  readonly updatedAt: string;
  readonly updatedBy: string;
  readonly recordedAt: string;
  readonly recordedBy: string;
}

/**
 * HTTP-Wrapper um die Branding-Endpunkte. Iteration 27 liefert
 * GET/PUT, Iteration 28f den Multipart-Asset-Upload fuer
 * LOGO/FAVICON/FONT.
 */
@Injectable({ providedIn: 'root' })
export class BrandingHttpService {
  private readonly api = inject(ApiClient);
  private readonly http = inject(HttpClient);

  load(): Promise<BrandingConfig> {
    return firstValueFrom(this.api.get<BrandingConfig>('/api/v1/theme'));
  }

  save(command: BrandingUpdateCommand): Promise<BrandingConfig> {
    return firstValueFrom(
      this.api.put<BrandingConfig, BrandingUpdateCommand>(
        '/api/v1/admin/theme',
        command
      )
    );
  }

  /**
   * Laedt eine Asset-Datei (Logo, Favicon, Font) auf den
   * Backend-Endpunkt {@code POST /api/v1/admin/theme/assets}.
   * Der Server pruft MIME-Typ, Groesse und bei SVG den Inhalt
   * (SvgSanitizer). Rueckgabe enthaelt die Asset-URL, die in der
   * Branding-Konfiguration verwendet werden kann.
   */
  uploadAsset(
    kind: BrandingAssetKind,
    file: File
  ): Promise<BrandingAssetResponse> {
    const form = new FormData();
    form.append('kind', kind);
    form.append('file', file, file.name);
    return firstValueFrom(
      this.http.post<BrandingAssetResponse>(
        this.api.url('/api/v1/admin/theme/assets'),
        form
      )
    );
  }

  /** UI-Fix MEDIUM-4: Liste historisierter Versionen (neueste zuerst). */
  history(limit = 20): Promise<BrandingHistoryEntry[]> {
    return firstValueFrom(
      this.api.get<BrandingHistoryEntry[]>(
        '/api/v1/admin/theme/history?limit=' + limit
      )
    );
  }

  /** UI-Fix MEDIUM-4: Ruecksetzen auf eine vergangene Version. */
  rollback(version: number): Promise<BrandingConfig> {
    return firstValueFrom(
      this.api.post<BrandingConfig, Record<string, never>>(
        '/api/v1/admin/theme/rollback/' + version,
        {}
      )
    );
  }
}
