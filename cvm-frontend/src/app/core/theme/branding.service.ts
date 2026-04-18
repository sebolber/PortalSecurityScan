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
}
