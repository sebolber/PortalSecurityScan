import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AppConfigService } from '../config/app-config.service';
import { ReportsService } from './reports.service';

describe('ReportsService', () => {
  let service: ReportsService;
  let http: HttpTestingController;

  beforeEach(() => {
    const config = jasmine.createSpyObj<AppConfigService>('AppConfigService', ['get']);
    config.get.and.returnValue({
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    } as any);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useValue: config }
      ]
    });
    service = TestBed.inject(ReportsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('POST erzeugt Hardening-Report ueber API', async () => {
    const promise = firstValueFrom(service.erzeuge({
      productVersionId: 'aa',
      environmentId: 'bb',
      gesamteinstufung: 'MEDIUM',
      erzeugtVon: 't.tester@ahs.test'
    }));
    const req = http.expectOne('http://api.test/api/v1/reports/hardening');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.gesamteinstufung).toBe('MEDIUM');
    req.flush({
      reportId: 'r1', productVersionId: 'aa', environmentId: 'bb',
      reportType: 'HARDENING', title: 't', gesamteinstufung: 'MEDIUM',
      erzeugtVon: 't', erzeugtAm: 'now', stichtag: 'now', sha256: 'abc'
    });
    const res = await promise;
    expect(res.reportId).toBe('r1');
  });

  it('GET ladePdf liefert Blob', async () => {
    const promise = firstValueFrom(service.ladePdf('r1'));
    const req = http.expectOne('http://api.test/api/v1/reports/r1');
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    const blob = new Blob([new Uint8Array([0x25, 0x50, 0x44, 0x46])],
        { type: 'application/pdf' });
    req.flush(blob);
    const out = await promise;
    expect(out instanceof Blob).toBeTrue();
  });
});
