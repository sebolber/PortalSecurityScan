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

export interface ProfileDiffEntry {
  readonly path: string;
  readonly oldValue: string | null;
  readonly newValue: string | null;
  readonly changeKind: string;
}

/**
 * HTTP-Wrapper um die Profil-Endpunkte. Neben der Lese-Fassung aus
 * Iteration 04 bietet der Service in Iteration 28b auch
 * Draft-Anlage, Approve und Diff fuer den YAML-Editor.
 */
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

  /**
   * Legt eine neue Draft-Version fuer die Umgebung an. Der Backend-
   * Service validiert das YAML gegen {@code profile-schema-v1.json};
   * bei Fehlern liefert er HTTP 400.
   */
  draftAnlegen(
    environmentId: string,
    yamlSource: string,
    proposedBy: string
  ): Promise<ProfileResponse> {
    return firstValueFrom(
      this.api.put<ProfileResponse, { yamlSource: string; proposedBy: string }>(
        `/api/v1/environments/${environmentId}/profile`,
        { yamlSource, proposedBy }
      )
    );
  }

  /** Aktiviert einen Draft im Vier-Augen-Prinzip. */
  freigeben(
    profileVersionId: string,
    approverId: string
  ): Promise<ProfileResponse> {
    return firstValueFrom(
      this.api.post<ProfileResponse, { approverId: string }>(
        `/api/v1/profiles/${profileVersionId}/approve`,
        { approverId }
      )
    );
  }

  /**
   * Feldweiser Diff gegen die aktuell aktive Version
   * ({@code against=latest}).
   */
  diffGegenAktiv(profileVersionId: string): Promise<ProfileDiffEntry[]> {
    return firstValueFrom(
      this.api.get<ProfileDiffEntry[]>(
        `/api/v1/profiles/${profileVersionId}/diff?against=latest`
      )
    );
  }

  /** Iteration 51 (CVM-101): DRAFT-YAML aktualisieren. */
  draftAktualisieren(
    profileVersionId: string,
    yamlSource: string,
    proposedBy: string
  ): Promise<ProfileResponse> {
    return firstValueFrom(
      this.api.put<ProfileResponse, { yamlSource: string; proposedBy: string }>(
        `/api/v1/profiles/${profileVersionId}`,
        { yamlSource, proposedBy }
      )
    );
  }

  /** Iteration 51 (CVM-101): Soft-Delete. ACTIVE-Versionen sind geschuetzt. */
  loesche(profileVersionId: string): Promise<void> {
    return firstValueFrom(
      this.api.delete<void>(`/api/v1/profiles/${profileVersionId}`)
    );
  }
}
