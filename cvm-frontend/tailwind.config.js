/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        // Top-Level-Aliases fuer Tailwind-Utilities wie bg-primary,
        // text-primary, border-primary. Binden an die CSS-Tokens aus
        // src/styles/tokens/colors.scss, damit der Mandanten-
        // ThemeService sie zur Laufzeit ueberschreiben kann.
        // Vorher: bg-primary rendert als transparent, weil primary
        // nur unter adesso.primary existierte - Freigeben-Button war
        // unsichtbar.
        primary: 'var(--color-primary)',
        'primary-contrast': 'var(--color-primary-contrast)',
        'primary-muted': 'var(--color-primary-muted)',
        surface: 'var(--color-surface)',
        'surface-muted': 'var(--color-surface-muted)',
        border: 'var(--color-border)',
        light: 'var(--color-surface-muted)',
        adesso: {
          primary: '#e2001a',
          primaryDark: '#b00015',
          dark: '#1c1c1c',
          surface: '#ffffff',
          surfaceDark: '#242424',
          light: '#f5f5f5'
        }
      }
    }
  },
  plugins: []
};
