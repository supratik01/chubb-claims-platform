import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClaimDetail } from '@claims/shared';
import { ClaimAction } from './claim-actions';

export interface ApprovalInput {
  readonly approvedAmount: number;
  readonly note: string | null;
}

/**
 * The workflow actions for one claim. Presentational: it validates its own forms and emits what the officer
 * decided; the page's store calls the API.
 */
@Component({
  selector: 'staff-claim-actions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './claim-actions.component.html',
  styleUrl: './claim-actions.component.scss',
})
export class ClaimActionsComponent {
  readonly claim = input.required<ClaimDetail>();
  readonly actions = input.required<readonly ClaimAction[]>();
  readonly busy = input(false);

  readonly pickUp = output<void>();
  readonly requestInformation = output<string>();
  readonly approve = output<ApprovalInput>();
  readonly reject = output<string>();
  readonly settle = output<void>();

  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly can = computed(() => new Set(this.actions()));
  protected readonly questionForm = this.fb.group({
    question: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(1000)]],
  });
  protected readonly decisionForm = this.fb.group({
    outcome: ['APPROVED' as 'APPROVED' | 'REJECTED'],
    approvedAmount: [null as number | null],
    reason: ['', Validators.maxLength(1000)],
  });
  protected readonly questionSubmitted = signal(false);
  protected readonly decisionSubmitted = signal(false);
  protected readonly outcome = signal<'APPROVED' | 'REJECTED'>('APPROVED');

  /** The approved amount may not exceed the sum insured. */
  protected readonly maxApprovable = computed(() => this.claim().sumInsured ?? null);
  private readonly claimId = computed(() => this.claim().id);

  constructor() {
    // Suggest the claimed amount, capped at the sum insured, when a claim is opened. This depends on the claim's
    // id only: the page refreshes the claim in the background, and that must not overwrite what the officer typed.
    effect(() => {
      this.claimId();
      const claim = untracked(this.claim);
      const cap = claim.sumInsured ?? claim.claimedAmount;
      this.decisionForm.controls.approvedAmount.setValue(Math.min(claim.claimedAmount, cap));
    });
    this.decisionForm.controls.outcome.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => this.outcome.set(value));
  }

  protected questionInvalid(): boolean {
    return this.questionSubmitted() && this.questionForm.invalid;
  }

  protected amountError(): string | null {
    if (!this.decisionSubmitted() || this.outcome() !== 'APPROVED') return null;
    const amount = this.decisionForm.controls.approvedAmount.value;
    if (amount == null || !(amount > 0)) return 'Enter an approved amount greater than zero.';
    const cap = this.maxApprovable();
    if (cap != null && amount > cap) return 'The approved amount cannot exceed the sum insured.';
    return null;
  }

  protected reasonError(): string | null {
    if (!this.decisionSubmitted()) return null;
    const reason = this.decisionForm.controls.reason.value.trim();
    if (this.outcome() === 'REJECTED' && reason.length === 0) return 'Give the reason for rejecting this claim.';
    if (reason.length > 1000) return 'Use at most 1000 characters.';
    return null;
  }

  protected sendQuestion(): void {
    this.questionSubmitted.set(true);
    if (this.questionForm.invalid) return;
    this.requestInformation.emit(this.questionForm.controls.question.value.trim());
    this.questionForm.reset();
    this.questionSubmitted.set(false);
  }

  protected recordDecision(): void {
    this.decisionSubmitted.set(true);
    if (this.amountError() || this.reasonError()) return;
    const { outcome, approvedAmount, reason } = this.decisionForm.getRawValue();
    const trimmed = reason.trim();
    if (outcome === 'APPROVED') {
      this.approve.emit({ approvedAmount: Number(approvedAmount), note: trimmed || null });
    } else {
      this.reject.emit(trimmed);
    }
  }
}
