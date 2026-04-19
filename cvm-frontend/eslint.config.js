const tsParser = require('@typescript-eslint/parser');
const angular = require('@angular-eslint/eslint-plugin');
const angularTemplate = require('@angular-eslint/eslint-plugin-template');
const angularTemplateParser = require('@angular-eslint/template-parser');

module.exports = [
  {
    files: ['src/**/*.ts'],
    languageOptions: {
      parser: tsParser,
      parserOptions: { ecmaVersion: 'latest', sourceType: 'module' }
    },
    plugins: { '@angular-eslint': angular },
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        { type: 'attribute', prefix: ['cvm', 'ahs'], style: 'camelCase' }
      ],
      '@angular-eslint/component-selector': [
        'error',
        { type: 'element', prefix: ['cvm', 'ahs'], style: 'kebab-case' }
      ],
      // Iteration 61H (CVM-62): Invariante gegen Rueckfaelle. Angular
      // Material und das Companion-CDK duerfen nach der Tailwind-
      // Migration nicht wieder importiert werden.
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            { group: ['@angular/material', '@angular/material/*'], message: 'Angular Material wurde in Iteration 61 entfernt. Verwende die Tailwind-Primitive in src/app/shared/components/.' },
            { group: ['@angular/cdk', '@angular/cdk/*'], message: 'Angular CDK wurde in Iteration 61 entfernt. Nutze die Tailwind-Komponenten.' },
            { group: ['material-icons', 'material-icons/*'], message: 'Icon-Font material-icons wurde entfernt. Verwende <cvm-icon name="..."> (lucide-angular).' }
          ]
        }
      ]
    }
  },
  {
    files: ['src/**/*.html'],
    languageOptions: { parser: angularTemplateParser },
    plugins: { '@angular-eslint/template': angularTemplate }
  }
];
