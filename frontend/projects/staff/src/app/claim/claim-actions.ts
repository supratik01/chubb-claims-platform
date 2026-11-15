import { ClaimSummary, UserSummary } from '@claims/shared';

export type ClaimAction = 'PICK_UP' | 'REQUEST_INFORMATION' | 'DECIDE' | 'SETTLE';

/**
 * Which workflow actions the signed-in staff member can take on a claim right now. It mirrors the backend's rules
 * (status transitions in ClaimStatus, assignment rules in ClaimAccessPolicy) so the screen only offers what will
 * be accepted. The backend still checks every action.
 */
export function availableActions(
  claim: Pick<ClaimSummary, 'status' | 'assignedOfficerId'>,
  user: Pick<UserSummary, 'id' | 'role'>,
): readonly ClaimAction[] {
  const mayAct = user.role === 'CLAIMS_MANAGER' || claim.assignedOfficerId === user.id;
  switch (claim.status) {
    case 'SUBMITTED':
      return ['PICK_UP'];
    case 'UNDER_REVIEW':
      return mayAct ? ['REQUEST_INFORMATION', 'DECIDE'] : [];
    case 'APPROVED':
      return mayAct ? ['SETTLE'] : [];
    default:
      return [];
  }
}

/** Why there is nothing to do, so the screen never shows an empty action panel without explanation. */
export function waitingReason(
  claim: Pick<ClaimSummary, 'status' | 'assignedOfficerName'>,
  actions: readonly ClaimAction[],
): string | null {
  if (actions.length > 0) return null;
  switch (claim.status) {
    case 'INFO_REQUESTED':
      return 'Waiting for the claimant to answer the open question.';
    case 'REJECTED':
      return 'This claim was rejected and is closed.';
    case 'SETTLED':
      return 'This claim has been paid and is closed.';
    default:
      return `This claim is assigned to ${claim.assignedOfficerName ?? 'another officer'}. Only they or a manager can act on it.`;
  }
}
