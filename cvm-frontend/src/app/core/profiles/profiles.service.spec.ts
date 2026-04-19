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
});
