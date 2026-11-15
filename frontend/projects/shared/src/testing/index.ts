/*
 * Test data builders shared by the unit tests of all four projects. Not part of the library's public API and never
 * imported by application code.
 */
import {
  ClaimDetail,
  ClaimSummary,
  InformationRequest,
  PolicySummary,
  UserSummary,
} from '../public-api';

export const CLAIMANT: UserSummary = {
  id: 'user-mei',
  displayName: 'Mei Ling Tan',
  role: 'CLAIMANT',
};
export const OFFICER: UserSummary = {
  id: 'user-sam',
  displayName: 'Sam Lee',
  role: 'CLAIMS_OFFICER',
};
export const OTHER_OFFICER: UserSummary = {
  id: 'user-priya',
  displayName: 'Priya Nair',
  role: 'CLAIMS_OFFICER',
};
export const MANAGER: UserSummary = {
  id: 'user-kenji',
  displayName: 'Kenji Sato',
  role: 'CLAIMS_MANAGER',
};

export function aPolicy(overrides: Partial<PolicySummary> = {}): PolicySummary {
  return {
    id: 'policy-1',
    policyNumber: 'POL-SG-M-10021',
    lineOfBusiness: 'MOTOR',
    countryCode: 'SG',
    currency: 'SGD',
    sumInsured: 80000,
    coverStart: '2026-01-01',
    coverEnd: '2026-12-31',
    ...overrides,
  };
}

export function aClaimSummary(overrides: Partial<ClaimSummary> = {}): ClaimSummary {
  return {
    id: 'claim-1',
    claimNumber: 'CLM-2026-000001',
    policyNumber: 'POL-SG-M-10021',
    lineOfBusiness: 'MOTOR',
    countryCode: 'SG',
    status: 'SUBMITTED',
    claimedAmount: 4200,
    currency: 'SGD',
    incidentDate: '2026-11-14',
    claimantName: 'Mei Ling Tan',
    assignedOfficerId: null,
    assignedOfficerName: null,
    submittedAt: '2026-11-15T01:00:00Z',
    updatedAt: '2026-11-15T01:00:00Z',
    ...overrides,
  };
}

export function aClaim(overrides: Partial<ClaimDetail> = {}): ClaimDetail {
  return {
    ...aClaimSummary(),
    description: 'Another car reversed into my parked car.',
    incidentLocation: 'Bukit Timah Road',
    sumInsured: 80000,
    reserveAmount: 4200,
    approvedAmount: null,
    decisionReason: null,
    statusHistory: [
      {
        fromStatus: null,
        toStatus: 'SUBMITTED',
        changedByName: 'Mei Ling Tan',
        note: null,
        changedAt: '2026-11-15T01:00:00Z',
      },
    ],
    informationRequests: [],
    ...overrides,
  };
}

export function anInformationRequest(
  overrides: Partial<InformationRequest> = {},
): InformationRequest {
  return {
    id: 'request-1',
    question: 'When was the pipe last serviced?',
    requestedByName: 'Sam Lee',
    requestedAt: '2026-11-15T02:00:00Z',
    response: null,
    respondedAt: null,
    ...overrides,
  };
}

/** An in-memory Storage, so session tests never touch the real browser storage. */
export class MemoryStorage implements Storage {
  private readonly items = new Map<string, string>();
  get length(): number {
    return this.items.size;
  }
  clear(): void {
    this.items.clear();
  }
  getItem(key: string): string | null {
    return this.items.get(key) ?? null;
  }
  key(index: number): string | null {
    return [...this.items.keys()][index] ?? null;
  }
  removeItem(key: string): void {
    this.items.delete(key);
  }
  setItem(key: string, value: string): void {
    this.items.set(key, value);
  }
}
