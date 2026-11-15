import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <section class="cc-page">
      <h1>Page not found</h1>
      <p>The page you asked for does not exist or has moved.</p>
      <a class="cc-button" routerLink="/">Go to your home page</a>
    </section>
  `,
})
export class NotFoundPage {}
