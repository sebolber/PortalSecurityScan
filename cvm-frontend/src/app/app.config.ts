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

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(APP_ROUTES),
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptor])),
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory: (config: AppConfigService) => () => config.load(),
      multi: true,
      deps: [AppConfigService]
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService, AppConfigService]
    },
    provideEchartsCore({ echarts })
  ]
};
