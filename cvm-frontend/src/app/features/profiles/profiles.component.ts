import { Component } from '@angular/core';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'cvm-profiles',
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <ahs-empty-state
      icon="tune"
      title="Profile"
      hint="Profil-Editor folgt in Iteration 18."
    ></ahs-empty-state>
  `
})
export class ProfilesComponent {}
