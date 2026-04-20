import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { monacoRouteProviders } from '../../shared/editor/monaco-providers';
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
import { CvmIconComponent } from '../../shared/components/cvm-icon.component';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

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
  // Iteration 57 (CVM-107): Side-by-Side-Diff (Monaco) anzeigen.
  diffOffen: boolean;
  // Iteration 64 (CVM-301): Editor auf bestehenden Draft mappen,
  // damit `draftSpeichern` statt einer neuen Version einen
  // `draftAktualisieren`-PUT schickt.
  bearbeitetDraft: boolean;
  deleting: boolean;
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
 * Vier-Augen-Prinzip aktivieren.
 */
@Component({
  selector: 'cvm-profiles',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MonacoEditorModule,
    AhsBannerComponent,
    CvmIconComponent
  ],
  // Iteration 54 (CVM-104): Monaco lazy in den Profiles-Chunk.
  providers: [monacoRouteProviders()],
  templateUrl: './profiles.component.html',
  styleUrls: ['./profiles.component.scss']
})
export class ProfilesComponent implements OnInit {
  private readonly envService = inject(EnvironmentsService);
  private readonly profileService = inject(ProfilesService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(CvmToastService);

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
        // Iteration 74 (CVM-311): persistenten DRAFT aus dem Backend
        // nachladen, damit der Editor nach Sessionwechsel weitermacht.
        const draft = await this.profileService
          .aktuellerDraft(env.id)
          .catch(() => null);
        let diff: readonly ProfileDiffEntry[] | null = null;
        if (draft) {
          diff = await this.profileService
            .diffGegenAktiv(draft.id)
            .catch(() => null);
        }
        rows.push({
          env,
          profile,
          draft,
          diff,
          editorOffen: false,
          editorYaml: profile?.yamlSource ?? DEFAULT_YAML,
          saving: false,
          approving: false,
          meldung: null,
          fehler: null,
          diffOffen: false,
          bearbeitetDraft: false,
          deleting: false
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
    const naechster = !row.editorOffen;
    this.patchRow(row, {
      editorOffen: naechster,
      fehler: null,
      meldung: null,
      // "Neuen Draft anlegen" oeffnet immer im Neu-Modus; beim
      // Zuklappen Flag zuruecksetzen, damit der naechste Einstieg
      // wieder unabhaengig startet.
      bearbeitetDraft: false,
      editorYaml: naechster
        ? row.profile?.yamlSource ?? DEFAULT_YAML
        : row.editorYaml
    });
  }

  /**
   * Iteration 64 (CVM-301): Bestehenden Draft zur Bearbeitung in
   * den Monaco-Editor laden. Ein Folge-Save fuehrt einen PUT-
   * Update aus statt eine neue Version anzulegen.
   */
  draftBearbeiten(row: ProfileRow): void {
    if (!row.draft) {
      return;
    }
    this.patchRow(row, {
      editorOffen: true,
      editorYaml: row.draft.yamlSource,
      bearbeitetDraft: true,
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
      const draft =
        row.bearbeitetDraft && row.draft
          ? await this.profileService.draftAktualisieren(
              row.draft.id,
              row.editorYaml,
              autor
            )
          : await this.profileService.draftAnlegen(
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
      const aktualisierung = row.bearbeitetDraft;
      this.patchRow(row, {
        draft,
        diff,
        saving: false,
        fehler: null,
        meldung:
          'Draft v' +
          draft.versionNumber +
          (aktualisierung
            ? ' aktualisiert. Freigabe durch anderen User noetig.'
            : ' gespeichert. Freigabe durch anderen User noetig.')
      });
      this.toast.success(
        aktualisierung ? 'Draft aktualisiert' : 'Draft angelegt',
        4000
      );
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

  /**
   * Iteration 64 (CVM-301): Soft-Delete eines Drafts. Das Backend
   * schuetzt ACTIVE-Versionen; hier behandeln wir nur den Draft-
   * Fall. Pattern folgt `AdminProductsComponent#loescheProdukt`
   * (window.confirm + Toast).
   */
  async draftLoeschen(row: ProfileRow): Promise<void> {
    if (!row.draft) {
      return;
    }
    if (!this.darfAutor()) {
      this.patchRow(row, { fehler: 'Rolle CVM_PROFILE_AUTHOR erforderlich.' });
      return;
    }
    const bestaetigt = window.confirm(
      'Draft v' +
        row.draft.versionNumber +
        ' der Umgebung ' +
        row.env.key +
        ' wirklich soft-loeschen?'
    );
    if (!bestaetigt) {
      return;
    }
    const versionNr = row.draft.versionNumber;
    const draftId = row.draft.id;
    this.patchRow(row, { deleting: true, fehler: null, meldung: null });
    try {
      await this.profileService.loesche(draftId);
      this.patchRow(row, {
        draft: null,
        diff: null,
        editorOffen: false,
        editorYaml: row.profile?.yamlSource ?? DEFAULT_YAML,
        bearbeitetDraft: false,
        deleting: false,
        meldung: 'Draft v' + versionNr + ' geloescht.'
      });
      this.toast.success('Draft geloescht', 4000);
    } catch (err) {
      this.patchRow(row, {
        deleting: false,
        fehler:
          err instanceof Error && err.message
            ? err.message
            : 'Loeschen fehlgeschlagen.'
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
      this.toast.success('Profil aktiviert', 4000);
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

  /**
   * Laedt ein kommentiertes Beispiel-Profil in den Editor. Der
   * Anwender soll sehen, welche Abschnitte es im Schema v1 gibt und
   * welche Flags typischerweise gesetzt werden. Key und Stage werden
   * aus der aktuellen Umgebung vorausgefuellt, damit die Vorlage
   * sofort gegen das Schema validiert.
   */
  vorlageEinfuegen(row: ProfileRow): void {
    this.patchRow(row, {
      editorYaml: this.buildVorlage(row.env.key, row.env.stage),
      fehler: null,
      meldung:
        'Vorlage geladen. Passe die Flags und Frameworks an die Umgebung an.'
    });
  }

  private buildVorlage(umgebungKey: string, stage: string): string {
    return (
      '# Beispiel-Kontextprofil (Schema v1).\n' +
      '# Passe die Flags unten an die tatsaechliche Umgebung an.\n' +
      '# Dokumentation: docs/initial/04-Kontextprofil.md\n' +
      'schemaVersion: 1\n' +
      '\n' +
      'umgebung:\n' +
      '  key: ' +
      umgebungKey +
      '\n' +
      '  stage: ' +
      stage +
      '\n' +
      '  # tenant: mein-tenant   # optional, nur bei Multi-Tenancy\n' +
      '\n' +
      '# Architektur der Umgebung (Boolean-Flags).\n' +
      'architecture:\n' +
      '  windows_hosts: false\n' +
      '  linux_hosts: true\n' +
      '  containerized: true\n' +
      '  kubernetes: true\n' +
      '  serverless: false\n' +
      '\n' +
      '# Netzwerk-Exposition.\n' +
      'network:\n' +
      '  internet_egress: false\n' +
      '  internet_ingress: false\n' +
      '  vpn_only: true\n' +
      '\n' +
      '# Hardening-Massnahmen, die in der Umgebung greifen.\n' +
      'hardening:\n' +
      '  tls_mtls: true\n' +
      '  waf: false\n' +
      '  secrets_in_vault: true\n' +
      '  patching_auto: true\n' +
      '\n' +
      '# Compliance-Rahmenwerke fuer diese Umgebung.\n' +
      'compliance:\n' +
      '  frameworks:\n' +
      '    - BSI_C5\n' +
      '    - ISO_27001\n'
    );
  }

  /** Iteration 57 (CVM-107): Monaco Side-by-Side Diff auf-/zuklappen. */
  diffUmschalten(row: ProfileRow): void {
    this.patchRow(row, { diffOffen: !row.diffOffen });
  }

  /**
   * Originaltext fuer den Diff (aktive YAML-Version) - left-side.
   */
  diffOriginal(row: ProfileRow): string {
    return row.profile?.yamlSource ?? '';
  }

  /**
   * Modifizierter Text fuer den Diff - right-side. Nutzt den
   * Draft-YAML, falls vorhanden, sonst den aktuellen Editor-Buffer.
   */
  diffModified(row: ProfileRow): string {
    return row.draft?.yamlSource ?? row.editorYaml ?? '';
  }

  private patchRow(row: ProfileRow, patch: Partial<ProfileRow>): void {
    this.rows.update((rows) =>
      rows.map((r) => (r.env.id === row.env.id ? { ...r, ...patch } : r))
    );
  }
}
