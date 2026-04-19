import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CvmBreadcrumbsComponent } from './cvm-breadcrumbs.component';
import { RoleMenuService } from '../../core/auth/role-menu.service';

/**
 * Iteration 91 (CVM-331): Breadcrumb-Rendering.
 */
describe('CvmBreadcrumbsComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CvmBreadcrumbsComponent],
      providers: [provideRouter([])]
    });
  });

  it('rendert nichts auf /dashboard (nur Start-Crumb)', () => {
    const router = TestBed.inject(Router);
    spyOnProperty(router, 'url', 'get').and.returnValue('/dashboard');
    const fixture = TestBed.createComponent(CvmBreadcrumbsComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.crumbs().length).toBe(1);
    const el = fixture.nativeElement.querySelector(
      '[data-testid="cvm-breadcrumbs"]'
    );
    expect(el).toBeNull();
  });

  it('/queue rendert Start > Bewertungs-Queue', () => {
    const router = TestBed.inject(Router);
    spyOnProperty(router, 'url', 'get').and.returnValue('/queue');
    const fixture = TestBed.createComponent(CvmBreadcrumbsComponent);
    fixture.detectChanges();
    const labels = fixture.componentInstance
      .crumbs()
      .map((c) => c.label);
    expect(labels).toEqual(['Start', 'Bewertungs-Queue']);
  });

  it('/admin/products rendert Start > Einstellungen > Produkte', () => {
    const router = TestBed.inject(Router);
    spyOnProperty(router, 'url', 'get').and.returnValue('/admin/products');
    const fixture = TestBed.createComponent(CvmBreadcrumbsComponent);
    fixture.detectChanges();
    const labels = fixture.componentInstance
      .crumbs()
      .map((c) => c.label);
    expect(labels).toEqual(['Start', 'Einstellungen', 'Produkte']);
  });

  it('/cves/CVE-2017-18640 rendert Start > CVEs (Prefix-Match)', () => {
    const router = TestBed.inject(Router);
    spyOnProperty(router, 'url', 'get').and.returnValue(
      '/cves/CVE-2017-18640'
    );
    const fixture = TestBed.createComponent(CvmBreadcrumbsComponent);
    fixture.detectChanges();
    const labels = fixture.componentInstance
      .crumbs()
      .map((c) => c.label);
    expect(labels).toEqual(['Start', 'CVEs']);
  });
});

describe('RoleMenuService.breadcrumbFor', () => {
  it('liefert nur Start fuer /dashboard', () => {
    const svc = new RoleMenuService();
    expect(svc.breadcrumbFor('/dashboard')).toEqual([
      { label: 'Start', path: '/dashboard' }
    ]);
  });

  it('haengt Parent Einstellungen fuer Admin-Unterpunkte an', () => {
    const svc = new RoleMenuService();
    const crumbs = svc.breadcrumbFor('/admin/products');
    expect(crumbs.map((c) => c.label)).toEqual([
      'Start',
      'Einstellungen',
      'Produkte'
    ]);
  });

  it('schneidet queryParams/fragment ab', () => {
    const svc = new RoleMenuService();
    const crumbs = svc.breadcrumbFor('/queue?status=APPROVED#row-1');
    expect(crumbs.map((c) => c.label)).toEqual(['Start', 'Bewertungs-Queue']);
  });
});
