import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Signal,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  ErrorAlertComponent,
  LINE_OF_BUSINESS_LABELS,
  PageStateComponent,
  PolicySummary,
  MoneyPipe,
} from '@claims/shared';
import { incidentDateValidator, latestDateAnywhere } from './incident-date.validator';
import { ReportIncidentStore } from './report-incident.store';

type Field = 'policyId' | 'incidentDate' | 'incidentLocation' | 'description' | 'claimedAmount';

@Component({
  selector: 'claimant-report-incident-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MoneyPipe,
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    ErrorAlertComponent,
    PageStateComponent,
  ],
  providers: [ReportIncidentStore],
  templateUrl: './report-incident.page.html',
})
export class ReportIncidentPage {
  protected readonly store = inject(ReportIncidentStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly lineLabels = LINE_OF_BUSINESS_LABELS;
  protected readonly maxIncidentDate = latestDateAnywhere(new Date());
  protected readonly submitted = signal(false);

  private readonly policyId = signal('');
  protected readonly selectedPolicy: Signal<PolicySummary | null> = computed(
    () => this.store.policies().find((policy) => policy.id === this.policyId()) ?? null,
  );

  protected readonly form = inject(FormBuilder).nonNullable.group({
    policyId: ['', Validators.required],
    incidentDate: ['', [Validators.required, incidentDateValidator(() => this.selectedPolicy())]],
    incidentLocation: ['', Validators.maxLength(200)],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    claimedAmount: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  constructor() {
    // A different policy has a different cover period, so the date must be checked again.
    this.form.controls.policyId.valueChanges.pipe(takeUntilDestroyed()).subscribe((policyId) => {
      this.policyId.set(policyId);
      this.form.controls.incidentDate.updateValueAndValidity({ emitEvent: false });
    });
  }

  protected invalid(field: Field): boolean {
    const control = this.form.controls[field];
    return control.invalid && (this.submitted() || control.touched);
  }

  protected hasError(field: Field, error: string): boolean {
    return this.invalid(field) && this.form.controls[field].hasError(error);
  }

  protected describedBy(field: Field, hint?: string): string | null {
    const ids = [hint, this.invalid(field) ? `${field}-error` : null].filter(Boolean);
    return ids.length > 0 ? ids.join(' ') : null;
  }

  protected submit(): void {
    this.submitted.set(true);
    if (this.form.invalid) {
      queueMicrotask(() =>
        this.host.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
      );
      return;
    }
    const value = this.form.getRawValue();
    this.store.submit(
      {
        policyId: value.policyId,
        incidentDate: value.incidentDate,
        incidentLocation: value.incidentLocation.trim() || undefined,
        description: value.description.trim(),
        claimedAmount: Number(value.claimedAmount),
      },
      (claim) => void this.router.navigate(['..', 'claims', claim.id], { relativeTo: this.route }),
    );
  }
}
