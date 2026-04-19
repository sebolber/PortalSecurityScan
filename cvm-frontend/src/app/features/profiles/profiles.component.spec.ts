import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProfilesComponent } from './profiles.component';
import { ProfilesService } from '../../core/profiles/profiles.service';
import { EnvironmentsService } from '../../core/environments/environments.service';
import { AuthService } from '../../core/auth/auth.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

class FakeEnv {
  list = () =>
    Promise.resolve([
      { id: 'e1', key: 'REF-TEST', stage: 'REF', description: null }
    ]);
}

class FakeAuth {
  loggedIn = () => true;
  userRoles = () => ['CVM_PROFILE_AUTHOR', 'CVM_PROFILE_APPROVER'];
  username = () => 't.tester@ahs.test';
  hasRole = () => true;
  refreshFromKeycloak(): void {}
  async login(): Promise<void> {}
  async logout(): Promise<void> {}
  async getToken(): Promise<string> {
    return '';
  }
}

class FakeToast {
  success = jasmine.createSpy('success');
  warning = jasmine.createSpy('warning');
  error = jasmine.createSpy('error');
}

describe('ProfilesComponent - Iteration 64 (Draft-Edit / Loeschen)', () => {
  let profiles: jasmine.SpyObj<ProfilesService>;
  let toast: FakeToast;

  beforeEach(() => {
    profiles = jasmine.createSpyObj<ProfilesService>('ProfilesService', [
      'aktivesProfil',
      'aktuellerDraft',
      'draftAnlegen',
      'draftAktualisieren',
      'freigeben',
      'diffGegenAktiv',
      'loesche'
    ]);
    profiles.aktivesProfil.and.returnValue(Promise.resolve(null));
    profiles.aktuellerDraft.and.returnValue(Promise.resolve(null));
    profiles.diffGegenAktiv.and.returnValue(Promise.resolve([]));
    toast = new FakeToast();

    TestBed.configureTestingModule({
      imports: [ProfilesComponent],
      providers: [
        provideRouter([]),
        { provide: ProfilesService, useValue: profiles },
        { provide: EnvironmentsService, useClass: FakeEnv },
        { provide: AuthService, useClass: FakeAuth },
        { provide: CvmToastService, useValue: toast }
      ]
    });
  });

  function draftResponse(id: string, version: number, yaml: string) {
    return {
      id,
      environmentId: 'e1',
      versionNumber: version,
      state: 'DRAFT',
      yamlSource: yaml,
      proposedBy: 't.tester@ahs.test',
      approvedBy: null,
      approvedAt: null,
      validFrom: '2026-04-19T00:00:00Z'
    };
  }

  async function ladeUndLegeDraftAn(): Promise<{
    component: ProfilesComponent;
  }> {
    const fixture = TestBed.createComponent(ProfilesComponent);
    const component = fixture.componentInstance;
    await component.laden();
    const row = component.rows()[0];
    profiles.draftAnlegen.and.returnValue(
      Promise.resolve(draftResponse('d1', 1, 'initial'))
    );
    component.editorInhalt(row, 'initial');
    await component.draftSpeichern(row);
    return { component };
  }

  it('draftBearbeiten: oeffnet den Editor mit Draft-YAML; Folge-Save ruft draftAktualisieren', async () => {
    const { component } = await ladeUndLegeDraftAn();
    const row = component.rows()[0];

    component.draftBearbeiten(row);
    const nachOeffnen = component.rows()[0];
    expect(nachOeffnen.editorOffen).toBeTrue();
    expect(nachOeffnen.editorYaml).toBe('initial');
    expect(nachOeffnen.bearbeitetDraft).toBeTrue();

    profiles.draftAktualisieren.and.returnValue(
      Promise.resolve(draftResponse('d1', 1, 'geaendert'))
    );
    component.editorInhalt(nachOeffnen, 'geaendert');
    await component.draftSpeichern(component.rows()[0]);

    expect(profiles.draftAktualisieren).toHaveBeenCalledOnceWith(
      'd1',
      'geaendert',
      't.tester@ahs.test'
    );
    expect(profiles.draftAnlegen).toHaveBeenCalledTimes(1);
    expect(toast.success).toHaveBeenCalledWith('Draft aktualisiert', 4000);
  });

  it('draftLoeschen: bricht bei window.confirm=false ab, ruft kein DELETE', async () => {
    const { component } = await ladeUndLegeDraftAn();
    spyOn(window, 'confirm').and.returnValue(false);

    await component.draftLoeschen(component.rows()[0]);
    expect(profiles.loesche).not.toHaveBeenCalled();
    expect(component.rows()[0].draft).not.toBeNull();
  });

  it('draftLoeschen: loescht Draft, setzt Row zurueck, zeigt Toast', async () => {
    const { component } = await ladeUndLegeDraftAn();
    spyOn(window, 'confirm').and.returnValue(true);
    profiles.loesche.and.returnValue(Promise.resolve());

    await component.draftLoeschen(component.rows()[0]);

    expect(profiles.loesche).toHaveBeenCalledOnceWith('d1');
    expect(component.rows()[0].draft).toBeNull();
    expect(component.rows()[0].editorOffen).toBeFalse();
    expect(component.rows()[0].meldung).toBe('Draft v1 geloescht.');
    expect(toast.success).toHaveBeenCalledWith('Draft geloescht', 4000);
  });

  it('editorUmschalten: oeffnet im Neu-Modus (bearbeitetDraft=false)', async () => {
    const { component } = await ladeUndLegeDraftAn();
    component.draftBearbeiten(component.rows()[0]);
    // Editor schliessen und neu oeffnen -> zurueck im Neu-Modus.
    component.editorUmschalten(component.rows()[0]);
    component.editorUmschalten(component.rows()[0]);
    const row = component.rows()[0];
    expect(row.editorOffen).toBeTrue();
    expect(row.bearbeitetDraft).toBeFalse();
  });
});
