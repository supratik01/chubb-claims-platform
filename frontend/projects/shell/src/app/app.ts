import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ROLE_LABELS, SessionStore } from '@claims/shared';
import { distinctUntilChanged, filter, map, skip } from 'rxjs';
import { navigationFor } from './navigation';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  private readonly main = viewChild.required<ElementRef<HTMLElement>>('main');

  protected readonly user = this.session.user;
  protected readonly roleLabel = computed(() => {
    const role = this.session.role();
    return role ? ROLE_LABELS[role] : '';
  });
  protected readonly links = computed(() => {
    const role = this.session.role();
    return role ? navigationFor(role) : [];
  });

  constructor() {
    // When the page changes, move focus to the main region so keyboard and screen-reader users start at the new
    // content rather than at the link they used. The first load is left alone, and so are changes to the query
    // string only (such as switching work-queue tabs), where focus must stay on the control the user is using.
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map((event) => pathOf(event.urlAfterRedirects)),
        distinctUntilChanged(),
        skip(1),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.main().nativeElement.focus({ preventScroll: false }));
  }

  protected skipToMain(event: Event): void {
    event.preventDefault();
    this.main().nativeElement.focus();
  }

  protected signOut(): void {
    this.session.signOut();
    void this.router.navigate(['/login']);
  }
}

/** The path of a router URL without its query string or fragment. */
export function pathOf(url: string): string {
  return url.split(/[?#]/, 1)[0];
}
