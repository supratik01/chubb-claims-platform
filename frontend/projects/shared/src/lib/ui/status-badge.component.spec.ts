import { TestBed } from '@angular/core/testing';
import { ClaimStatus } from '../api/models';
import { StatusBadgeComponent } from './status-badge.component';

describe('StatusBadgeComponent', () => {
  it.each<[ClaimStatus, string]>([
    ['SUBMITTED', 'Submitted'],
    ['UNDER_REVIEW', 'Under review'],
    ['INFO_REQUESTED', 'Information requested'],
    ['APPROVED', 'Approved'],
    ['REJECTED', 'Rejected'],
    ['SETTLED', 'Settled'],
  ])('shows %s as the words "%s", not only a colour', async (status, label) => {
    const fixture = TestBed.createComponent(StatusBadgeComponent);
    fixture.componentRef.setInput('status', status);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent?.trim()).toBe(label);
  });
});
