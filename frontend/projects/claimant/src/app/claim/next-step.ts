import { ClaimStatus } from '@claims/shared';

/**
 * What the claimant should expect next, in plain words. Claims take days or weeks, so every status says whose
 * move it is.
 */
export function nextStepFor(status: ClaimStatus): string {
  switch (status) {
    case 'SUBMITTED':
      return 'We have received your claim. A claims officer will pick it up shortly.';
    case 'UNDER_REVIEW':
      return 'A claims officer is reviewing your claim. You do not need to do anything right now.';
    case 'INFO_REQUESTED':
      return 'We need more information from you. Please answer the question below so we can continue.';
    case 'APPROVED':
      return 'Your claim has been approved. Payment is being arranged.';
    case 'REJECTED':
      return 'Your claim has not been approved. The reason is shown below.';
    case 'SETTLED':
      return 'Payment has been made and this claim is closed.';
  }
}
