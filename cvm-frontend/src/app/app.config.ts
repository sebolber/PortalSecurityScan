import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { KeycloakService } from 'keycloak-angular';
import { provideEchartsCore } from 'ngx-echarts';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { TooltipComponent, LegendComponent } from 'echarts/components';
import { APP_ROUTES } from './app.routes';
import { AppConfigService } from './core/config/app-config.service';
import { authInterceptor } from './core/auth/auth.interceptor';
import { initializeKeycloak } from './core/auth/keycloak-init';

echarts.use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent]);

/**
 * Wichtig: Angular fuehrt {@link APP_INITIALIZER}-Eintraege parallel
 * aus. Wir brauchen aber Sequenz: erst {@code config.json} laden, dann
 * Keycloak mit den geladenen Werten initialisieren. Daher EIN
 * Initializer, der die Promises selbst verkettet.
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
    },
    provideEchartsCore({ echarts })
  ]
};
