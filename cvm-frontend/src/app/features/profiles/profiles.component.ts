import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/auth/auth.service';
import { CVM_ROLES } from '../../core/auth/cvm-roles';
import {
  EnvironmentView,
  EnvironmentsService
} from '../../core/environments/environments.service';
import {
  ProfileDiffEntry,
  ProfileResponse,
  ProfilesService
} from '../../core/profiles/profiles.service';
import { AhsBannerComponent } from '../../shared/components/ahs-banner.component';

interface ProfileRow {
  env: EnvironmentView;
  profile: ProfileResponse | null;
  draft: ProfileResponse | null;
  diff: readonly ProfileDiffEntry[] | null;
  editorOffen: boolean;
  editorYaml: string;
  saving: boolean;
  approving: boolean;
  meldung: string | null;
  fehler: string | null;
}

const DEFAULT_YAML = `schemaVersion: 1
umgebung:
  key: REF-TEST
  stage: REF
architecture:
  windows_hosts: false
  containerized: true
network:
  internet_egress: false
hardening:
  tls_mtls: true
compliance:
  frameworks:
    - BSI_C5
`;

/**
 * Profil-Verwaltung (Iteration 04 + 28b, CVM-66).
 *
 * <p>Pro Umgebung: aktives Profil anzeigen, neuen Draft in einer
 * Textarea bearbeiten, gegen die aktive Version diffen, Draft im
 * Vier-Augen-Prinzip aktivieren. Monaco-Editor ist als Folge-
 * Iteration vorgesehen (angular.json-Asset-Setup noetig), bis
 * dahin reicht eine Textarea mit Mono-Font.
 */
@Component({
  selector: 'cvm-profiles',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    AhsBannerComponent
  ],
  templateUrl: './profiles.component.html',
  styleUrls: ['./profiles.component.scss']
})
export class ProfilesComponent implements OnInit {
  private readonly envService = inject(EnvironmentsService);
  private readonly profileService = inject(ProfilesService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly rows = signal<ProfileRow[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly darfAutor = computed(
    () =>
      this.auth.hasRole(CVM_ROLES.PROFILE_AUTHOR) ||
      this.auth.hasRole(CVM_ROLES.ADMIN)
  );
  readonly darfApprove = computed(
    () =>
      this.auth.hasRole(CVM_ROLES.PROFILE_APPROVER) ||
      this.auth.hasRole(CVM_ROLES.ADMIN)
  );

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const envs = await this.envService.list();
      const rows: ProfileRow[] = [];
      for (const env of envs) {
        const profile = await this.profileService
          .aktivesProfil(env.id)
          .catch(() => null);
        rows.push({
          env,
          profile,
          draft: null,
          diff: null,
          editorOffen: false,
          editorYaml: profile?.yamlSource ?? DEFAULT_YAML,
          saving: false,
          approving: false,
          meldung: null,
          fehler: null
        });
      }
      this.rows.set(rows);
    } catch {
      this.error.set('Umgebungen/Profile konnten nicht geladen werden.');
    } finally {
      this.loading.set(false);
    }
  }

  editorUmschalten(row: ProfileRow): void {
    this.patchRow(row, {
      editorOffen: !row.editorOffen,
      fehler: null,
      meldung: null
    });
  }

  editorInhalt(row: ProfileRow, yaml: string): void {
    this.patchRow(row, { editorYaml: yaml });
  }

  async draftSpeichern(row: ProfileRow): Promise<void> {
    if (!this.darfAutor()) {
      this.patchRow(row, { fehler: 'Rolle CVM_PROFILE_AUTHOR erforderlich.' });
      return;
    }
    const autor = this.auth.username() || 'unbekannt';
    this.patchRow(row, { saving: true, fehler: null, meldung: null });
    try {
      const draft = await this.profileService.draftAnlegen(
        row.env.id,
        row.editorYaml,
        autor
      );
      let diff: readonly ProfileDiffEntry[] | null = null;
      try {
        diff = await this.profileService.diffGegenAktiv(draft.id);
      } catch {
        diff = null;
      }
      this.patchRow(row, {
        draft,
        diff,
        saving: false,
        meldung:
          'Draft v' +
          draft.versionNumber +
          ' gespeichert. Freigabe durch anderen User noetig.'
      });
      this.snackBar.open('Draft angelegt', 'OK', { duration: 4000 });
    } catch (err) {
      this.patchRow(row, {
        saving: false,
        fehler:
          err instanceof Error && err.message
            ? err.message
            : 'YAML konnte nicht akzeptiert werden.'
      });
    }
  }

  async draftFreigeben(row: ProfileRow): Promise<void> {
    if (!row.draft) {
      return;
    }
    if (!this.darfApprove()) {
      this.patchRow(row, {
        fehler: 'Rolle CVM_PROFILE_APPROVER erforderlich.'
      });
      return;
    }
    const approver = this.auth.username() || 'unbekannt';
    this.patchRow(row, { approving: true, fehler: null });
    try {
      const aktiv = await this.profileService.freigeben(row.draft.id, approver);
      this.patchRow(row, {
        profile: aktiv,
        draft: null,
        diff: null,
        approving: false,
        editorOffen: false,
        editorYaml: aktiv.yamlSource,
        meldung: 'Profil-Version ' + aktiv.versionNumber + ' aktiv.'
      });
      this.snackBar.open('Profil aktiviert', 'OK', { duration: 4000 });
    } catch (err) {
      this.patchRow(row, {
        approving: false,
        fehler:
          err instanceof Error && err.message
            ? err.message
            : 'Freigabe fehlgeschlagen (Vier-Augen?).'
      });
    }
  }

  zuruecksetzen(row: ProfileRow): void {
    this.patchRow(row, {
      editorYaml: row.profile?.yamlSource ?? DEFAULT_YAML,
      fehler: null,
      meldung: null
    });
  }

  private patchRow(row: ProfileRow, patch: Partial<ProfileRow>): void {
    this.rows.update((rows) =>
      rows.map((r) => (r.env.id === row.env.id ? { ...r, ...patch } : r))
    );
  }
}
