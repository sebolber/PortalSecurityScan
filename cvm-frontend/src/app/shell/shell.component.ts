import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

interface MenuEntry {
  readonly label: string;
  readonly path: string;
  readonly icon: string;
  readonly disabled: boolean;
}

@Component({
  selector: 'cvm-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss']
})
export class ShellComponent {
  readonly menu: readonly MenuEntry[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'dashboard', disabled: false },
    { label: 'Bewertungen', path: '/queue', icon: 'rule', disabled: true },
    { label: 'Profile', path: '/profiles', icon: 'tune', disabled: true },
    { label: 'Reports', path: '/reports', icon: 'description', disabled: true }
  ];
}
