import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AhsBannerComponent } from './ahs-banner.component';

@Component({
  standalone: true,
  imports: [AhsBannerComponent],
  template: `
    <ahs-banner [kind]="kind" [title]="title">Kostencap erreicht</ahs-banner>
  `
})
class HostComponent {
  kind: 'info' | 'warn' | 'critical' | 'success' = 'warn';
  title = 'Achtung';
}

describe('AhsBannerComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  it('projiziert Slot-Inhalt und Titel', () => {
    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('Achtung');
    expect(text).toContain('Kostencap erreicht');
  });

  it('setzt banner-Klasse anhand des Input-Props (Iteration 61A)', () => {
    const banner = fixture.nativeElement.querySelector('.banner');
    expect(banner.classList.contains('banner-warning')).toBeTrue();

    fixture.componentInstance.kind = 'critical';
    fixture.detectChanges();
    expect(banner.classList.contains('banner-critical')).toBeTrue();
  });
});
