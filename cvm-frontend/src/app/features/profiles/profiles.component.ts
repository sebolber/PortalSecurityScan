import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import {
  EnvironmentView,
  EnvironmentsService
} from '../../core/environments/environments.service';
import {
  ProfileResponse,
  ProfilesService
} from '../../core/profiles/profiles.service';

interface ProfileRow {
  readonly env: EnvironmentView;
  readonly profile: ProfileResponse | null;
}

@Component({
  selector: 'cvm-profiles',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatExpansionModule,
    MatIconModule
  ],
  templateUrl: './profiles.component.html',
  styleUrls: ['./profiles.component.scss']
})
export class ProfilesComponent implements OnInit {
  private readonly envService = inject(EnvironmentsService);
  private readonly profileService = inject(ProfilesService);

  readonly rows = signal<readonly ProfileRow[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    void this.laden();
  }

  async laden(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const envs = await this.envService.list();
      const rows: ProfileRow[] = [];
      for (const env of envs) {
        const profile = await this.profileService
          .aktivesProfil(env.id)
          .catch(() => null);
        rows.push({ env, profile });
      }
      this.rows.set(rows);
    } catch {
      this.error.set('Umgebungen/Profile konnten nicht geladen werden.');
    } finally {
      this.loading.set(false);
    }
  }
}
