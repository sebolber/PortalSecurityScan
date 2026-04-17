import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'cvm-dashboard',
  standalone: true,
  imports: [MatCardModule],
  template: `
    <mat-card class="max-w-xl">
      <mat-card-header>
        <mat-card-title>Dashboard (Platzhalter)</mat-card-title>
        <mat-card-subtitle>Iteration 00 – Shell ist lauffaehig.</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <p>
          Fachliche Inhalte folgen ab Iteration 07 (Frontend-Shell) und
          Iteration 08 (Bewertungs-Queue-UI).
        </p>
      </mat-card-content>
    </mat-card>
  `
})
export class DashboardComponent {}
