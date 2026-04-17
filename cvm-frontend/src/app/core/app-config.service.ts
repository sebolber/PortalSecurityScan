import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface AppConfig {
  readonly apiBaseUrl: string;
  readonly keycloak: {
    readonly url: string;
    readonly realm: string;
    readonly clientId: string;
  };
}

@Injectable({ providedIn: 'root' })
export class AppConfigService {
  private config: AppConfig | null = null;

  constructor(private readonly http: HttpClient) {}

  async load(): Promise<AppConfig> {
    const loaded = await firstValueFrom(this.http.get<AppConfig>('assets/config.json'));
    this.config = loaded;
    return loaded;
  }

  get(): AppConfig {
    if (!this.config) {
      throw new Error('AppConfig wurde noch nicht geladen.');
    }
    return this.config;
  }
}
