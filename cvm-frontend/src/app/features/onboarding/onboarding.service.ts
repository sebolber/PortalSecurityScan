import { Injectable, signal } from '@angular/core';

export type OnboardingStepId = 'produkt' | 'umgebung' | 'profil' | 'scan';

export interface OnboardingState {
  readonly done: readonly OnboardingStepId[];
  readonly current: OnboardingStepId;
}

const STORAGE_KEY = 'cvm.onboarding.v1';
const ORDER: readonly OnboardingStepId[] = [
  'produkt',
  'umgebung',
  'profil',
  'scan'
];

function initialState(): OnboardingState {
  if (typeof localStorage === 'undefined') {
    return { done: [], current: 'produkt' };
  }
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return { done: [], current: 'produkt' };
  }
  try {
    const parsed = JSON.parse(raw) as Partial<OnboardingState>;
    const done = Array.isArray(parsed.done)
      ? parsed.done.filter((s): s is OnboardingStepId => ORDER.includes(s as OnboardingStepId))
      : [];
    const current: OnboardingStepId =
      typeof parsed.current === 'string' && ORDER.includes(parsed.current as OnboardingStepId)
        ? (parsed.current as OnboardingStepId)
        : 'produkt';
    return { done, current };
  } catch {
    return { done: [], current: 'produkt' };
  }
}

/**
 * Iteration 96 (CVM-336): Erstnutzer-Wizard. Persistiert den
 * Zustand in localStorage, damit der Admin seine Onboarding-Schritte
 * ueber Sessions hinweg behalten kann.
 */
@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly stateSig = signal<OnboardingState>(initialState());

  readonly state = this.stateSig.asReadonly();

  markDone(step: OnboardingStepId): void {
    const aktuell = this.stateSig();
    if (aktuell.done.includes(step)) {
      return;
    }
    const done = [...aktuell.done, step];
    const next = this.naechster(done);
    this.persist({ done, current: next });
  }

  reset(): void {
    this.persist({ done: [], current: 'produkt' });
  }

  completed(): boolean {
    return ORDER.every((s) => this.stateSig().done.includes(s));
  }

  private naechster(done: readonly OnboardingStepId[]): OnboardingStepId {
    const rest = ORDER.filter((s) => !done.includes(s));
    return rest[0] ?? 'scan';
  }

  private persist(next: OnboardingState): void {
    this.stateSig.set(next);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    }
  }
}
