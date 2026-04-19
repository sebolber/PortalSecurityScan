import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppConfigService } from '../config/app-config.service';
import { HttpErrorHandler } from '../api/http-error-handler';
import { ProfilesService } from './profiles.service';

describe('ProfilesService', () => {
  let service: ProfilesService;
  let http: HttpTestingController;
  let errorHandler: jasmine.SpyObj<HttpErrorHandler>;

  beforeEach(() => {
    const config = jasmine.createSpyObj<AppConfigService>(
      'AppConfigService',
      ['get']
    );
    config.get.and.returnValue({
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    } as any);
    errorHandler = jasmine.createSpyObj<HttpErrorHandler>('HttpErrorHandler', [
      'show'
    ]);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useValue: config },
        { provide: HttpErrorHandler, useValue: errorHandler }
      ]
    });
    service = TestBed.inject(ProfilesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('diffGegenAktiv: 200 mit Liste -> Promise loest mit Liste auf', async () => {
    const promise = service.diffGegenAktiv('v1');
    const req = http.expectOne(
      'http://api.test/api/v1/profiles/v1/diff?against=latest'
    );
    expect(req.request.method).toBe('GET');
    req.flush([
      { path: 'umgebung.key', oldValue: 'A', newValue: 'B', changeKind: 'CHANGED' }
    ]);
    const res = await promise;
    expect(res.length).toBe(1);
    expect(errorHandler.show).not.toHaveBeenCalled();
  });

  it('diffGegenAktiv: 404 -> liefert leere Liste und keinen Error-Toast', async () => {
    const promise = service.diffGegenAktiv('v1');
    const req = http.expectOne(
      'http://api.test/api/v1/profiles/v1/diff?against=latest'
    );
    req.flush(
      { error: 'profile_not_found', message: 'Profil unbekannt' },
      { status: 404, statusText: 'Not Found' }
    );
    const res = await promise;
    expect(res).toEqual([]);
    expect(errorHandler.show).not.toHaveBeenCalled();
  });

  it('diffGegenAktiv: 500 -> wirft und meldet beim HttpErrorHandler', async () => {
    const promise = service.diffGegenAktiv('v1');
    const req = http.expectOne(
      'http://api.test/api/v1/profiles/v1/diff?against=latest'
    );
    req.flush(
      { error: 'server_error' },
      { status: 500, statusText: 'Internal Server Error' }
    );
    await expectAsync(promise).toBeRejected();
    expect(errorHandler.show).toHaveBeenCalled();
  });

  it('draftAktualisieren: PUT /api/v1/profiles/{id} mit YAML und Autor', async () => {
    const promise = service.draftAktualisieren('v1', 'y1', 't.tester@ahs.test');
    const req = http.expectOne('http://api.test/api/v1/profiles/v1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      yamlSource: 'y1',
      proposedBy: 't.tester@ahs.test'
    });
    req.flush({
      id: 'v1',
      environmentId: 'e1',
      versionNumber: 5,
      state: 'DRAFT',
      yamlSource: 'y1',
      proposedBy: 't.tester@ahs.test',
      approvedBy: null,
      approvedAt: null,
      validFrom: '2026-04-19T00:00:00Z'
    });
    const res = await promise;
    expect(res.state).toBe('DRAFT');
    expect(res.versionNumber).toBe(5);
  });

  it('loesche: DELETE /api/v1/profiles/{id}', async () => {
    const promise = service.loesche('v1');
    const req = http.expectOne('http://api.test/api/v1/profiles/v1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    await promise;
  });

  it('aktuellerDraft: 200 mit DRAFT -> liefert Response', async () => {
    const promise = service.aktuellerDraft('env-1');
    const req = http.expectOne(
      'http://api.test/api/v1/environments/env-1/profile/draft'
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'd1',
      environmentId: 'env-1',
      versionNumber: 3,
      state: 'DRAFT',
      yamlSource: 'schemaVersion: 1',
      proposedBy: 't.tester@ahs.test',
      approvedBy: null,
      approvedAt: null,
      validFrom: '2026-04-19T00:00:00Z'
    });
    const res = await promise;
    expect(res).not.toBeNull();
    expect(res?.state).toBe('DRAFT');
    expect(errorHandler.show).not.toHaveBeenCalled();
  });

  it('aktuellerDraft: 404 -> liefert null ohne Error-Toast', async () => {
    const promise = service.aktuellerDraft('env-1');
    const req = http.expectOne(
      'http://api.test/api/v1/environments/env-1/profile/draft'
    );
    req.flush(
      { error: 'profile_not_found' },
      { status: 404, statusText: 'Not Found' }
    );
    const res = await promise;
    expect(res).toBeNull();
    expect(errorHandler.show).not.toHaveBeenCalled();
  });
});
