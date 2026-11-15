import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ApiError } from '../api/api-error';

/** Shows a failed command's message and any field errors. Announced to screen readers as it appears. */
@Component({
  selector: 'cc-error-alert',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error(); as error) {
      <div class="alert" role="alert">
        <p class="alert__message">{{ error.message }}</p>
        @if (error.fieldErrors.length > 0) {
          <ul>
            @for (fieldError of error.fieldErrors; track fieldError.field + fieldError.message) {
              <li>{{ fieldError.field }}: {{ fieldError.message }}</li>
            }
          </ul>
        }
      </div>
    }
  `,
  styles: `
    .alert {
      color: var(--cc-tone-danger-fg);
      background: var(--cc-tone-danger-bg);
      border-left: 4px solid var(--cc-color-danger);
      border-radius: var(--cc-radius);
      padding: var(--cc-space-3) var(--cc-space-4);
      margin-bottom: var(--cc-space-4);
    }
    .alert__message {
      margin: 0;
      font-weight: 600;
    }
    ul {
      margin: var(--cc-space-2) 0 0;
      padding-left: var(--cc-space-6);
    }
  `,
})
export class ErrorAlertComponent {
  readonly error = input<ApiError | null>(null);
}
