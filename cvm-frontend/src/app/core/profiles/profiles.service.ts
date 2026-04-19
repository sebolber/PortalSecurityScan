import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
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
   * angelegt wurde. Iteration 61 (CVM-62): nutzt `getOptional`, damit
   * 404 keinen Error-Toast im UI ausloest - "kein Profil" ist ein
   * legitimer Leer-Zustand, kein Fehler.
   */
  aktivesProfil(environmentId: string): Promise<ProfileResponse | null> {
    return firstValueFrom(
      this.api.getOptional<ProfileResponse>(
        `/api/v1/environments/${environmentId}/profile`
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
   *
   * <p>Iteration 63 (CVM-300): nutzt `getOptional`, damit ein 404
   * (Profil-ID unbekannt) keinen globalen Fehler-Toast ausloest.
   * Fachlich "noch keine Vorgaengerversion" liefert das Backend seit
   * Iteration 63 ohnehin HTTP 200 mit leerer Liste.
   */
  async diffGegenAktiv(
    profileVersionId: string
  ): Promise<ProfileDiffEntry[]> {
    const ergebnis = await firstValueFrom(
      this.api.getOptional<ProfileDiffEntry[]>(
        `/api/v1/profiles/${profileVersionId}/diff?against=latest`
      )
    );
    return ergebnis ?? [];
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
