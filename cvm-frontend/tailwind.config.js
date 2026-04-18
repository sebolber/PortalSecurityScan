/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
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
