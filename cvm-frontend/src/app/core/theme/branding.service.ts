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

/**
 * HTTP-Wrapper um die Branding-Endpunkte (Iteration 27).
 */
@Injectable({ providedIn: 'root' })
export class BrandingHttpService {
  private readonly api = inject(ApiClient);

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
}
