import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RoleMenuService } from '../../core/auth/role-menu.service';
import { CvmIconComponent } from './cvm-icon.component';

/**
 * Iteration 91 (CVM-331): Breadcrumb-Pfad zur aktuellen Seite.
 * Rendert eine `nav`-Liste `Start > [Parent >] <aktuelle Seite>`;
 * letzter Eintrag ist ein Text (kein Link), Zwischenebenen sind
 * Router-Links.
 */
@Component({
  selector: 'cvm-breadcrumbs',
  standalone: true,
  imports: [CommonModule, RouterLink, CvmIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (crumbs().length > 1) {
      <nav aria-label="Breadcrumb" class="cvm-breadcrumbs"
           data-testid="cvm-breadcrumbs">
        <ol class="flex items-center gap-1 text-xs text-text-muted">
          @for (c of crumbs(); track c.path; let last = $last) {
            <li class="flex items-center gap-1">
              @if (!last) {
                <a [routerLink]="c.path"
                   class="hover:text-primary underline-offset-2 hover:underline">
                  {{ c.label }}
                </a>
                <cvm-icon name="chevron-right" [size]="12"></cvm-icon>
              } @else {
                <span class="text-text-primary font-medium">{{ c.label }}</span>
              }
            </li>
          }
        </ol>
      </nav>
    }
  `
})
export class CvmBreadcrumbsComponent {
  private readonly router = inject(Router);
  private readonly menu = inject(RoleMenuService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly currentUrl = signal<string>(this.router.url);

  readonly crumbs = computed(() => this.menu.breadcrumbFor(this.currentUrl()));

  constructor() {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((e) => this.currentUrl.set(e.urlAfterRedirects));
  }
}
