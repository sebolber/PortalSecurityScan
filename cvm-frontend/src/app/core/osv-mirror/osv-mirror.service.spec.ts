import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppConfigService } from '../config/app-config.service';
import { HttpErrorHandler } from '../api/http-error-handler';
import { OsvMirrorService } from './osv-mirror.service';

describe('OsvMirrorService', () => {
  let service: OsvMirrorService;
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
    service = TestBed.inject(OsvMirrorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('reload(): POST /api/v1/admin/osv-mirror/reload liefert indexSize', async () => {
    const promise = service.reload();
    const req = http.expectOne('http://api.test/api/v1/admin/osv-mirror/reload');
    expect(req.request.method).toBe('POST');
    req.flush({ reloaded: true, indexSize: 42 });
    const res = await promise;
    expect(res.reloaded).toBeTrue();
    expect(res.indexSize).toBe(42);
  });

  it('reload(): 503 -> wirft und meldet beim HttpErrorHandler', async () => {
    const promise = service.reload();
    const req = http.expectOne('http://api.test/api/v1/admin/osv-mirror/reload');
    req.flush(
      { error: 'osv_mirror_inactive' },
      { status: 503, statusText: 'Service Unavailable' }
    );
    await expectAsync(promise).toBeRejected();
    expect(errorHandler.show).toHaveBeenCalled();
  });
});
