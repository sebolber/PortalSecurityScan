import { Provider } from '@angular/core';
import { provideEchartsCore } from 'ngx-echarts';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import {
  BarChart,
  LineChart,
  PieChart
} from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  TooltipComponent
} from 'echarts/components';

/**
 * Iteration 52 (CVM-102): ECharts wird nur in chart-tragenden Routen
 * geladen (Dashboard, Tenant-KPI). Der Aufrufer traegt diesen Provider
 * im {@code providers:}-Array seiner Route ein; dadurch landet echarts
 * nicht im Initial-Bundle.
 *
 * <p>Die Komponenten-Auswahl (Bar/Line/Pie + Grid/Legend/Tooltip) deckt
 * den aktuellen Bedarf ab und bleibt klein. Neue Chart-Typen bitte
 * hier eintragen, nicht per Default-Import.
 */
export function echartsRouteProviders(): Provider[] {
  echarts.use([
    CanvasRenderer,
    BarChart,
    LineChart,
    PieChart,
    GridComponent,
    LegendComponent,
    TooltipComponent
  ]);
  return [provideEchartsCore({ echarts })];
}
