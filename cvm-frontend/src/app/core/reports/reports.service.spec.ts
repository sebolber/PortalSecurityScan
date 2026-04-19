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

  it('Iteration 93: list() fuellt queryParams korrekt', async () => {
    const promise = firstValueFrom(
      service.list({ productVersionId: 'pv1', size: 5 })
    );
    const req = http.expectOne(
      'http://api.test/api/v1/reports?productVersionId=pv1&size=5'
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      items: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0
    });
    const out = await promise;
    expect(out.totalElements).toBe(0);
  });

  it('Iteration 93: list() ohne Filter ruft /api/v1/reports', async () => {
    const promise = firstValueFrom(service.list());
    const req = http.expectOne('http://api.test/api/v1/reports');
    req.flush({
      items: [
        {
          reportId: 'r1',
          productVersionId: 'pv1',
          environmentId: 'e1',
          reportType: 'HARDENING',
          title: 'T',
          gesamteinstufung: 'HIGH',
          erzeugtVon: 't',
          erzeugtAm: 'now',
          stichtag: 'now',
          sha256: 'abc'
        }
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1
    });
    const out = await promise;
    expect(out.items.length).toBe(1);
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
