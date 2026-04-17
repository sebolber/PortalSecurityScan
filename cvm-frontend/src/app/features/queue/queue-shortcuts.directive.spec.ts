import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QueueShortcutsDirective } from './queue-shortcuts.directive';

@Component({
  standalone: true,
  imports: [QueueShortcutsDirective],
  template: `
    <div
      cvmQueueShortcuts
      (next)="counts.next = counts.next + 1"
      (previous)="counts.previous = counts.previous + 1"
      (approve)="counts.approve = counts.approve + 1"
      (override)="counts.override = counts.override + 1"
      (reject)="counts.reject = counts.reject + 1"
      (help)="counts.help = counts.help + 1"
    ></div>
    <input #eingabe />
  `
})
class HostComponent {
  @ViewChild('eingabe') eingabe!: { nativeElement: HTMLInputElement };
  counts = {
    next: 0,
    previous: 0,
    approve: 0,
    override: 0,
    reject: 0,
    help: 0
  };
}

function druecke(key: string, target: EventTarget | null = document.body): void {
  const event = new KeyboardEvent('keydown', { key, bubbles: true });
  (target ?? document).dispatchEvent(event);
}

describe('QueueShortcutsDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('j/k steuern Naechster/Vorheriger', () => {
    druecke('j');
    druecke('k');
    expect(host.counts.next).toBe(1);
    expect(host.counts.previous).toBe(1);
  });

  it('a/o/r loesen Approve/Override/Reject aus', () => {
    druecke('a');
    druecke('o');
    druecke('r');
    expect(host.counts.approve).toBe(1);
    expect(host.counts.override).toBe(1);
    expect(host.counts.reject).toBe(1);
  });

  it('? oeffnet Hilfe', () => {
    druecke('?');
    expect(host.counts.help).toBe(1);
  });

  it('ignoriert Shortcuts in Input-Feldern', () => {
    const input = host.eingabe.nativeElement;
    input.focus();
    druecke('a', input);
    expect(host.counts.approve).toBe(0);
  });

  it('ignoriert Shortcuts mit Modifier', () => {
    const event = new KeyboardEvent('keydown', { key: 'a', ctrlKey: true });
    document.dispatchEvent(event);
    expect(host.counts.approve).toBe(0);
  });
});
