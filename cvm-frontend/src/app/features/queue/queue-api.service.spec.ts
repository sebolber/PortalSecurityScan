import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { QueueApiService } from './queue-api.service';
import { ApiClient } from '../../core/api/api-client.service';
import { AppConfigService } from '../../core/config/app-config.service';
import { HttpErrorHandler } from '../../core/api/http-error-handler';

class FakeConfig {
  get() {
    return {
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    };
  }
}

class FakeErrorHandler {
  show = jasmine.createSpy('show');
  format = () => '';
}

describe('QueueApiService', () => {
  let service: QueueApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ApiClient,
        QueueApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useClass: FakeConfig },
        { provide: HttpErrorHandler, useClass: FakeErrorHandler }
      ]
    });
    service = TestBed.inject(QueueApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list ohne Filter ruft die Queue ohne Query-Parameter', () => {
    service.list({}).subscribe();
    const req = httpMock.expectOne('http://api.test/api/v1/findings');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('list mit Filter serialisiert Query-Parameter', () => {
    service
      .list({
        status: 'PROPOSED',
        productVersionId: '11111111-1111-1111-1111-111111111111',
        environmentId: '22222222-2222-2222-2222-222222222222',
        source: 'RULE'
      })
      .subscribe();
    // Der Service baut die Query direkt in den URL-String ein (statt
    // ueber HttpParams), damit ApiClient#get(path) keine zweite
    // Signatur braucht. Wir pruefen daher die finale URL, nicht
    // r.params - das war im Originaltest falsch und blieb bis
    // Iteration 32 unbemerkt, weil Karma ohne Chromium nie lief.
    const req = httpMock.expectOne((r) =>
      r.urlWithParams.startsWith('http://api.test/api/v1/findings?')
      && r.urlWithParams.includes('status=PROPOSED')
      && r.urlWithParams.includes(
          'productVersionId=11111111-1111-1111-1111-111111111111')
      && r.urlWithParams.includes(
          'environmentId=22222222-2222-2222-2222-222222222222')
      && r.urlWithParams.includes('source=RULE')
    );
    req.flush([]);
  });

  it('approve postet gegen den Assessment-Endpunkt', () => {
    service.approve('aaa', { approverId: 'bbb' }).subscribe();
    const req = httpMock.expectOne('http://api.test/api/v1/assessments/aaa/approve');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ approverId: 'bbb' });
    req.flush({});
  });

  it('reject postet Kommentar mit', () => {
    service
      .reject('aaa', { approverId: 'bbb', comment: 'nicht relevant' })
      .subscribe();
    const req = httpMock.expectOne('http://api.test/api/v1/assessments/aaa/reject');
    expect(req.request.body).toEqual({
      approverId: 'bbb',
      comment: 'nicht relevant'
    });
    req.flush({});
  });
});
