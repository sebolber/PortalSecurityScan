/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  // Iteration 61A (CVM-62): Dark-Mode ausschliesslich ueber `data-theme='dark'`.
  // Doppel-Mechanismus (class + attribut) entfaellt mit Material-Ausbau.
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Fira Sans"', 'system-ui', 'sans-serif'],
        heading: ['"Fira Sans Condensed"', '"Fira Sans"', 'system-ui', 'sans-serif'],
        mono: ['"Fira Code"', '"Fira Mono"', 'ui-monospace', 'monospace']
      },
      colors: {
        // Alles bindet an CSS-Custom-Properties aus src/styles/tokens/.
        // Mandanten-ThemeService kann die Werte zur Laufzeit ueberschreiben.
        primary: {
          DEFAULT: 'var(--color-primary)',
          contrast: 'var(--color-primary-contrast)',
          hover: 'var(--color-primary-hover)',
          pressed: 'var(--color-primary-pressed)',
          muted: 'var(--color-primary-muted)'
        },
        surface: {
          DEFAULT: 'var(--color-surface)',
          raised: 'var(--color-surface-raised)',
          muted: 'var(--color-surface-muted)'
        },
        border: {
          DEFAULT: 'var(--color-border)',
          strong: 'var(--color-border-strong)'
        },
        text: {
          DEFAULT: 'var(--color-text)',
          muted: 'var(--color-text-muted)',
          subtle: 'var(--color-text-subtle, var(--color-text-muted))',
          inverse: 'var(--color-text-inverse)'
        },
        focus: 'var(--color-focus)',
        severity: {
          critical: 'var(--color-severity-critical-bg)',
          'critical-fg': 'var(--color-severity-critical-fg)',
          high: 'var(--color-severity-high-bg)',
          'high-fg': 'var(--color-severity-high-fg)',
          medium: 'var(--color-severity-medium-bg)',
          'medium-fg': 'var(--color-severity-medium-fg)',
          low: 'var(--color-severity-low-bg)',
          'low-fg': 'var(--color-severity-low-fg)',
          informational: 'var(--color-severity-informational-bg)',
          'informational-fg': 'var(--color-severity-informational-fg)'
        }
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-lg)',
        '2xl': '16px',
        pill: 'var(--radius-pill)'
      },
      boxShadow: {
        xs: 'var(--shadow-xs)',
        sm: 'var(--shadow-sm)',
        md: 'var(--shadow-md)',
        lg: 'var(--shadow-lg)',
        card: 'var(--shadow-sm)'
      },
      spacing: {
        // Alias-Skala auf die Token-Werte, damit Entwickler sowohl
        // Tailwind-Zahlen (p-4) als auch Tokens (p-space-4) verwenden koennen.
        'space-1': 'var(--space-1)',
        'space-2': 'var(--space-2)',
        'space-3': 'var(--space-3)',
        'space-4': 'var(--space-4)',
        'space-5': 'var(--space-5)',
        'space-6': 'var(--space-6)',
        'space-7': 'var(--space-7)',
        'space-8': 'var(--space-8)'
      },
      fontSize: {
        xs: ['var(--text-xs)', { lineHeight: 'var(--line-height-snug)' }],
        sm: ['var(--text-sm)', { lineHeight: 'var(--line-height-normal)' }],
        base: ['var(--text-base)', { lineHeight: 'var(--line-height-normal)' }],
        lg: ['var(--text-lg)', { lineHeight: 'var(--line-height-snug)' }],
        xl: ['var(--text-xl)', { lineHeight: 'var(--line-height-snug)' }],
        '2xl': ['var(--text-2xl)', { lineHeight: 'var(--line-height-tight)' }],
        '3xl': ['var(--text-3xl)', { lineHeight: 'var(--line-height-tight)' }]
      },
      transitionDuration: {
        fast: 'var(--duration-fast)',
        base: 'var(--duration-base)',
        slow: 'var(--duration-slow)'
      },
      transitionTimingFunction: {
        standard: 'var(--easing-standard)',
        emphasized: 'var(--easing-emphasized)'
      },
      // ringColor erbt aus `colors`-Extend oben, damit `ring-primary`,
      // `ring-primary-muted`, `ring-focus` etc. alle verfuegbar sind.
    }
  },
  plugins: [require('@tailwindcss/forms')({ strategy: 'class' })]
};
