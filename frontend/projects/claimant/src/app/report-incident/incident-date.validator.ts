import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { PolicySummary } from '@claims/shared';

/**
 * Checks the incident date against today and against the chosen policy's cover period, so the claimant learns
 * about the problem before submitting. The server applies the same rules; this is a convenience, not the check.
 *
 * "Today" is the latest calendar date anywhere (UTC+14), matching the backend, so a claimant in a time zone ahead
 * of the browser's is never told that today is in the future.
 */
export function incidentDateValidator(policy: () => PolicySummary | null, now: () => Date = () => new Date()): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const value = control.value;
    if (!value) return null;
    if (value > latestDateAnywhere(now())) return { future: true };
    const selected = policy();
    if (selected && (value < selected.coverStart || value > selected.coverEnd)) {
      return { outsideCover: { start: selected.coverStart, end: selected.coverEnd } };
    }
    return null;
  };
}

export function latestDateAnywhere(now: Date): string {
  const fourteenHoursAhead = new Date(now.getTime() + 14 * 60 * 60 * 1000);
  return fourteenHoursAhead.toISOString().slice(0, 10);
}
