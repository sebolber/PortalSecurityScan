import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { KeycloakService } from 'keycloak-angular';
import { APP_ROUTES } from './app.routes';
import { AppConfigService } from './core/config/app-config.service';
import { authInterceptor } from './core/auth/auth.interceptor';
import { initializeKeycloak } from './core/auth/keycloak-init';

/**
 * Wichtig: Angular fuehrt {@link APP_INITIALIZER}-Eintraege parallel
 * aus. Wir brauchen aber Sequenz: erst {@code config.json} laden, dann
 * Keycloak mit den geladenen Werten initialisieren. Daher EIN
 * Initializer, der die Promises selbst verkettet.
 *
 * <p>Iteration 52 (CVM-102): {@code provideEchartsCore} ist bewusst
 * NICHT mehr hier. ECharts wird nur in den Routen {@code dashboard}
 * und {@code tenant-kpi} via Route-Provider lazy geladen, damit die
 * initial-Bundle-Groesse unter das Budget faellt.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(APP_ROUTES),
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptor])),
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory:
        (config: AppConfigService, keycloak: KeycloakService) => async () => {
          await config.load();
          await initializeKeycloak(keycloak, config)();
          return true;
        },
      multi: true,
      deps: [AppConfigService, KeycloakService]
    }
  ]
};
