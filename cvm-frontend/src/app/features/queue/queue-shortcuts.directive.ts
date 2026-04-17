import {
  Directive,
  EventEmitter,
  HostListener,
  Output
} from '@angular/core';

/**
 * Tastatur-Shortcuts fuer die Queue. Die Direktive haengt am
 * Root-Element der Queue-Page. Ereignisse werden ueber
 * {@link Output}s ausgeleitet, damit Anwendung und Store testbar
 * bleiben.
 *
 * <p>Shortcuts schlucken KEINE Events, wenn der Fokus in einem
 * Editier-Element liegt &ndash; so bleiben Eingabefelder,
 * Textareas, Comboboxen und {@code contenteditable}-Bloecke frei
 * bedienbar.
 */
@Directive({
  selector: '[cvmQueueShortcuts]',
  standalone: true
})
export class QueueShortcutsDirective {
  @Output() readonly next = new EventEmitter<void>();
  @Output() readonly previous = new EventEmitter<void>();
  @Output() readonly approve = new EventEmitter<void>();
  @Output() readonly override = new EventEmitter<void>();
  @Output() readonly reject = new EventEmitter<void>();
  @Output() readonly help = new EventEmitter<void>();

  @HostListener('document:keydown', ['$event'])
  handleKey(event: KeyboardEvent): void {
    if (this.shouldIgnore(event)) {
      return;
    }
    switch (event.key) {
      case 'j':
        this.emit(event, this.next);
        return;
      case 'k':
        this.emit(event, this.previous);
        return;
      case 'a':
        this.emit(event, this.approve);
        return;
      case 'o':
        this.emit(event, this.override);
        return;
      case 'r':
        this.emit(event, this.reject);
        return;
      case '?':
        this.emit(event, this.help);
        return;
      default:
        return;
    }
  }

  private shouldIgnore(event: KeyboardEvent): boolean {
    if (event.ctrlKey || event.metaKey || event.altKey) {
      return true;
    }
    const target = event.target as HTMLElement | null;
    if (!target) {
      return false;
    }
    if (target.isContentEditable) {
      return true;
    }
    const tag = target.tagName;
    return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';
  }

  private emit(event: KeyboardEvent, emitter: EventEmitter<void>): void {
    event.preventDefault();
    emitter.emit();
  }
}
