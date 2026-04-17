/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        adesso: {
          primary: '#e2001a',
          dark: '#1c1c1c',
          light: '#f5f5f5'
        }
      }
    }
  },
  plugins: []
};
