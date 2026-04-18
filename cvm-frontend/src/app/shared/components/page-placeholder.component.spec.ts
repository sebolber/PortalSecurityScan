import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PagePlaceholderComponent } from './page-placeholder.component';

describe('PagePlaceholderComponent', () => {
  let fixture: ComponentFixture<PagePlaceholderComponent>;
  let component: PagePlaceholderComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PagePlaceholderComponent]
    });
    fixture = TestBed.createComponent(PagePlaceholderComponent);
    component = fixture.componentInstance;
    component.title = 'Anomalie-Board';
    component.description = 'Die KI-Anomalien erscheinen hier.';
    component.iteration = 'Iteration 27b';
    component.ticket = 'CVM-62';
    fixture.detectChanges();
  });

  it('rendert Titel, Beschreibung und Iterations-Referenz', () => {
    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Anomalie-Board');
    expect(root.textContent).toContain('Die KI-Anomalien erscheinen hier.');
    expect(root.textContent).toContain('Iteration 27b');
    expect(root.textContent).toContain('CVM-62');
  });

  it('verwendet data-testid=cvm-page-placeholder als Gate-Marker', () => {
    const marker = fixture.nativeElement.querySelector(
      '[data-testid="cvm-page-placeholder"]'
    );
    expect(marker).not.toBeNull();
  });
});
