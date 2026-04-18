import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, firstValueFrom, of } from 'rxjs';
import { ApiClient } from '../api/api-client.service';

export interface ProfileResponse {
  readonly id: string;
  readonly environmentId: string;
  readonly versionNumber: number;
  readonly state: string;
  readonly yamlSource: string;
  readonly proposedBy: string | null;
  readonly approvedBy: string | null;
  readonly approvedAt: string | null;
  readonly validFrom: string | null;
}

/** Liest die aktive Profil-Version pro Umgebung. */
@Injectable({ providedIn: 'root' })
export class ProfilesService {
  private readonly api = inject(ApiClient);

  /**
   * Liefert das aktive Profil, oder {@code null}, wenn noch keines
   * angelegt wurde (404 vom Backend).
   */
  aktivesProfil(environmentId: string): Promise<ProfileResponse | null> {
    return firstValueFrom(
      this.api
        .get<ProfileResponse>(`/api/v1/environments/${environmentId}/profile`)
        .pipe(
          catchError((err: HttpErrorResponse) => {
            if (err.status === 404) {
              return of(null as ProfileResponse | null);
            }
            throw err;
          })
        )
    );
  }
}
