import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ScanUploadComponent } from './scan-upload.component';
import { AppConfigService } from '../../core/config/app-config.service';
import { CvmToastService } from '../../shared/components/cvm-toast.service';

class FakeToast {
  success = jasmine.createSpy('success');
  warning = jasmine.createSpy('warning');
  error = jasmine.createSpy('error');
  info = jasmine.createSpy('info');
}

describe('ScanUploadComponent - Iteration 80 Workflow-CTAs', () => {
  beforeEach(() => {
    const config = jasmine.createSpyObj<AppConfigService>(
      'AppConfigService',
      ['get']
    );
    config.get.and.returnValue({
      apiBaseUrl: 'http://api.test',
      keycloak: { url: '', realm: '', clientId: '' }
    } as any);
    TestBed.configureTestingModule({
      imports: [ScanUploadComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AppConfigService, useValue: config },
        { provide: CvmToastService, useClass: FakeToast }
      ]
    });
  });

  it('zeigt nach Summary "Zur Queue"-Button mit productVersionId', () => {
    const fixture = TestBed.createComponent(ScanUploadComponent);
    const comp = fixture.componentInstance;
    comp.selectedVersionId.set('pv-1');
    comp.selectedEnvironmentId.set('env-1');
    comp.summary.set({
      scanId: 's1',
      productVersionId: 'pv-1',
      environmentId: 'env-1',
      scanner: 'trivy',
      contentSha256: 'abc',
      scannedAt: '2026-04-19T00:00:00Z',
      componentCount: 5,
      findingCount: 2
    });
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector(
      'a[data-testid="scan-upload-to-queue"]'
    ) as HTMLAnchorElement;
    expect(link).withContext('Queue-CTA sichtbar').not.toBeNull();
    // queryParams werden vom RouterLink in das href uebernommen.
    expect(link.getAttribute('href')).toContain('productVersionId=pv-1');
    expect(link.getAttribute('href')).toContain('environmentId=env-1');

    const dashLink = fixture.nativeElement.querySelector(
      'a[data-testid="scan-upload-to-dashboard"]'
    );
    expect(dashLink).not.toBeNull();
  });

  it('ohne Version keine Queue-CTA; Dashboard-CTA weiterhin da', () => {
    const fixture = TestBed.createComponent(ScanUploadComponent);
    const comp = fixture.componentInstance;
    comp.selectedVersionId.set(null);
    comp.summary.set({
      scanId: 's1',
      productVersionId: 'pv-1',
      environmentId: 'env-1',
      scanner: 'trivy',
      contentSha256: 'abc',
      scannedAt: '2026-04-19T00:00:00Z',
      componentCount: 5,
      findingCount: 2
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector(
      'a[data-testid="scan-upload-to-queue"]'
    )).toBeNull();
    expect(fixture.nativeElement.querySelector(
      'a[data-testid="scan-upload-to-dashboard"]'
    )).not.toBeNull();
  });
});
