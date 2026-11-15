import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Shown in place of a micro-frontend that could not be loaded, so one failing area never blanks the app. */
@Component({
  selector: 'app-remote-unavailable-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="cc-page" role="alert">
      <h1>This area is temporarily unavailable</h1>
      <p>We could not load this part of the application. Your work has not been lost.</p>
      <button type="button" class="cc-button" (click)="reload()">Try again</button>
    </section>
  `,
})
export class RemoteUnavailablePage {
  protected reload(): void {
    location.reload();
  }
}
