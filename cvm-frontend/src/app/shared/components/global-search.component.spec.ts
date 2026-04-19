import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { GlobalSearchComponent } from './global-search.component';
import { AuthService } from '../../core/auth/auth.service';

class FakeAuth {
  loggedIn = () => true;
  userRoles = () => ['CVM_ADMIN'];
  username = () => 'a.admin@ahs.test';
  hasRole = () => true;
  refreshFromKeycloak(): void {}
  async login(): Promise<void> {}
  async logout(): Promise<void> {}
  async getToken(): Promise<string> {
    return '';
  }
}

/**
 * Iteration 92 (CVM-332): GlobalSearchComponent.
 */
describe('GlobalSearchComponent', () => {
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.returnValue(Promise.resolve(true));

    TestBed.configureTestingModule({
      imports: [GlobalSearchComponent],
      providers: [
        provideRouter([]),
        { provide: Router, useValue: router },
        { provide: AuthService, useClass: FakeAuth }
      ]
    });
  });

  it('liefert keine Treffer bei leerer Eingabe', () => {
    const component = TestBed.createComponent(GlobalSearchComponent)
      .componentInstance;
    component.setQuery('');
    expect(component.hits().length).toBe(0);
  });

  it('filtert Menue-Eintraege substring-match (case-insensitive)', () => {
    const component = TestBed.createComponent(GlobalSearchComponent)
      .componentInstance;
    component.setQuery('queue');
    const labels = component.hits().map((h) => h.label);
    expect(labels).toContain('Bewertungs-Queue');
  });

  it('bietet CVE-ID-Direkttreffer, wenn Input dem Pattern entspricht', () => {
    const component = TestBed.createComponent(GlobalSearchComponent)
      .componentInstance;
    component.setQuery('CVE-2017-18640');
    const hits = component.hits();
    expect(hits[0].kind).toBe('cve');
    expect(hits[0].path).toBe('/cves/CVE-2017-18640');
  });

  it('normalisiert CVE-ID in Uppercase', () => {
    const component = TestBed.createComponent(GlobalSearchComponent)
      .componentInstance;
    component.setQuery('cve-2026-22610');
    expect(component.hits()[0].path).toBe('/cves/CVE-2026-22610');
  });

  it('waehle ruft navigateByUrl und leert den Query', () => {
    const component = TestBed.createComponent(GlobalSearchComponent)
      .componentInstance;
    component.setQuery('queue');
    const first = component.hits()[0];
    component.waehle(first);
    expect(router.navigateByUrl).toHaveBeenCalledWith(first.path);
    expect(component.query()).toBe('');
  });

  it('waehleErsten navigiert zum ersten Treffer', () => {
    const component = TestBed.createComponent(GlobalSearchComponent)
      .componentInstance;
    component.setQuery('Dashboard');
    component.waehleErsten();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('waehleErsten ohne Treffer ist noop', () => {
    const component = TestBed.createComponent(GlobalSearchComponent)
      .componentInstance;
    component.setQuery('gibtsnicht');
    component.waehleErsten();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
