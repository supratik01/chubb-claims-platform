import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Root component for running this micro-frontend on its own during development. */
@Component({
  selector: 'claimant-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet],
  template: `<main><router-outlet /></main>`,
})
export class App {}
