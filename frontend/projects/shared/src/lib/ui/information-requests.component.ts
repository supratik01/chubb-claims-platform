import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, input, output, untracked } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { InformationRequest } from '../api/models';

export interface InformationAnswer {
  readonly requestId: string;
  readonly response: string;
}

/**
 * The questions staff asked and the claimant's answers. Read-only for staff; the claimant can answer the open
 * question. The component only emits the answer: the micro-frontend's store sends it.
 */
@Component({
  selector: 'cc-information-requests',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, ReactiveFormsModule],
  template: `
    @if (requests().length === 0) {
      <p class="cc-muted">No information has been requested.</p>
    } @else {
      <ul class="requests">
        @for (request of requests(); track request.id) {
          <li class="request">
            <p class="request__question">{{ request.question }}</p>
            <p class="request__meta">
              Asked by {{ request.requestedByName }} on
              <time [attr.datetime]="request.requestedAt">{{ request.requestedAt | date: 'medium' }}</time>
            </p>
            @if (request.response) {
              <p class="request__answer"><strong>Answer:</strong> {{ request.response }}</p>
            } @else if (canRespond()) {
              <form class="request__form" (ngSubmit)="submit(request.id)">
                <div class="cc-field">
                  <label [for]="'answer-' + request.id">Your answer</label>
                  <textarea
                    [id]="'answer-' + request.id"
                    [formControl]="answer"
                    maxlength="2000"
                    [attr.aria-invalid]="showError() ? 'true' : null"
                    [attr.aria-describedby]="showError() ? 'answer-error-' + request.id : null"
                  ></textarea>
                  @if (showError()) {
                    <span class="cc-field__error" [id]="'answer-error-' + request.id">Please enter an answer.</span>
                  }
                </div>
                <button type="submit" class="cc-button" [disabled]="busy()">
                  {{ busy() ? 'Sending…' : 'Send answer' }}
                </button>
              </form>
            } @else {
              <p class="request__pending">Awaiting the claimant's answer.</p>
            }
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .requests { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--cc-space-4); }
    .request { border-left: 4px solid var(--cc-color-border); padding-left: var(--cc-space-4); }
    .request__question { font-weight: 600; margin: 0; }
    .request__meta { color: var(--cc-color-text-muted); font-size: 0.875rem; margin: var(--cc-space-1) 0; }
    .request__answer { margin: var(--cc-space-2) 0 0; }
    .request__pending { color: var(--cc-tone-warning-fg); margin: var(--cc-space-2) 0 0; font-weight: 600; }
    .request__form { margin-top: var(--cc-space-3); }
  `,
})
export class InformationRequestsComponent {
  readonly requests = input.required<readonly InformationRequest[]>();
  readonly canRespond = input(false);
  readonly busy = input(false);
  readonly respond = output<InformationAnswer>();

  protected readonly answer = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(2000)],
  });
  private readonly hasOpenRequest = computed(() => this.requests().some((request) => !request.response));
  private submitted = false;

  constructor() {
    // Once the open question has been answered, clear the text so a later question starts empty.
    effect(() => {
      if (!this.hasOpenRequest()) {
        untracked(() => this.answer.reset());
        this.submitted = false;
      }
    });
  }

  protected showError(): boolean {
    return this.submitted && this.answer.invalid;
  }

  protected submit(requestId: string): void {
    this.submitted = true;
    const response = this.answer.value.trim();
    if (this.answer.invalid || response.length === 0) {
      this.answer.setErrors({ required: true });
      return;
    }
    this.respond.emit({ requestId, response });
  }
}
