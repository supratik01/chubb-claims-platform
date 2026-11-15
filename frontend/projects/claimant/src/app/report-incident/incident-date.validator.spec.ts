import { FormControl } from '@angular/forms';
import { aPolicy } from '@claims/shared/testing';
import { incidentDateValidator, latestDateAnywhere } from './incident-date.validator';

describe('incidentDateValidator', () => {
  const now = () => new Date('2026-11-15T10:00:00Z');
  const policy = aPolicy({ coverStart: '2026-01-01', coverEnd: '2026-12-31' });
  const check = (date: string) =>
    new FormControl(
      date,
      incidentDateValidator(() => policy, now),
    ).errors;

  it('accepts a date inside the cover period that is not in the future', () => {
    expect(check('2026-11-14')).toBeNull();
  });

  it('refuses a date in the future', () => {
    expect(check('2026-11-20')).toEqual({ future: true });
  });

  it('refuses a date before the cover started', () => {
    expect(check('2025-12-31')).toEqual({
      outsideCover: { start: '2026-01-01', end: '2026-12-31' },
    });
  });

  it("accepts tomorrow's date while it is already tomorrow somewhere, as the backend does", () => {
    // 10:00 UTC on 15 November is already 16 November in UTC+14.
    expect(latestDateAnywhere(now())).toBe('2026-11-16');
    expect(check('2026-11-16')).toBeNull();
  });
});
