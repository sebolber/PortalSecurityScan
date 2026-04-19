import { Provider } from '@angular/core';
import {
  NGX_MONACO_EDITOR_CONFIG,
  NgxMonacoEditorConfig
} from 'ngx-monaco-editor-v2';

/**
 * Iteration 54 (CVM-104): Monaco-Editor wird nur auf der Profile-Seite
 * geladen. Der Loader-URL zeigt auf die per angular.json ausgespielten
 * Monaco-Assets unter {@code /assets/monaco/vs}.
 *
 * <p>Wir registrieren die Konfiguration als Component-scoped Provider,
 * damit Monaco nicht im Initial-Bundle landet, sondern nur wenn die
 * Profile-Route aktiviert ist.
 */
const PROFILES_MONACO_CONFIG: NgxMonacoEditorConfig = {
  baseUrl: 'assets/monaco',
  defaultOptions: {
    scrollBeyondLastLine: false,
    minimap: { enabled: false },
    automaticLayout: true,
    tabSize: 2,
    theme: 'vs'
  }
};

export function monacoRouteProviders(): Provider[] {
  return [
    {
      provide: NGX_MONACO_EDITOR_CONFIG,
      useValue: PROFILES_MONACO_CONFIG
    }
  ];
}
