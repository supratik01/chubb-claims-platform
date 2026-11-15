import { ClaimStatus, LineOfBusiness, Role } from '../api/models';

export const CLAIM_STATUS_LABELS: Readonly<Record<ClaimStatus, string>> = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  INFO_REQUESTED: 'Information requested',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  SETTLED: 'Settled',
};

export const LINE_OF_BUSINESS_LABELS: Readonly<Record<LineOfBusiness, string>> = {
  MOTOR: 'Motor',
  PROPERTY: 'Property',
};

export const ROLE_LABELS: Readonly<Record<Role, string>> = {
  CLAIMANT: 'Claimant',
  CLAIMS_OFFICER: 'Claims officer',
  CLAIMS_MANAGER: 'Claims manager',
};

/** Whose move it is: Info Requested is the only status in which the claim waits on the claimant. */
export function isAwaitingClaimant(status: ClaimStatus): boolean {
  return status === 'INFO_REQUESTED';
}

export function isClosed(status: ClaimStatus): boolean {
  return status === 'REJECTED' || status === 'SETTLED';
}
