import { ClaimStatus } from '@claims/shared';
import { MANAGER, OFFICER, OTHER_OFFICER } from '@claims/shared/testing';
import { availableActions, waitingReason } from './claim-actions';

describe('availableActions', () => {
  const claim = (status: ClaimStatus, assignedOfficerId: string | null = OFFICER.id) => ({
    status,
    assignedOfficerId,
  });

  it('lets any staff member pick up a new claim', () => {
    expect(availableActions(claim('SUBMITTED', null), OTHER_OFFICER)).toEqual(['PICK_UP']);
  });

  it('lets the assigned officer ask for information or decide while the claim is under review', () => {
    expect(availableActions(claim('UNDER_REVIEW'), OFFICER)).toEqual([
      'REQUEST_INFORMATION',
      'DECIDE',
    ]);
  });

  it('gives another officer no actions on a claim assigned to someone else', () => {
    expect(availableActions(claim('UNDER_REVIEW'), OTHER_OFFICER)).toEqual([]);
  });

  it('lets a manager act on any claim', () => {
    expect(availableActions(claim('APPROVED'), MANAGER)).toEqual(['SETTLE']);
  });

  it.each<ClaimStatus>(['INFO_REQUESTED', 'REJECTED', 'SETTLED'])(
    'offers nothing while the claim is %s',
    (status) => expect(availableActions(claim(status), OFFICER)).toEqual([]),
  );
});

describe('waitingReason', () => {
  it('says the claim is waiting on the claimant', () => {
    expect(
      waitingReason({ status: 'INFO_REQUESTED', assignedOfficerName: 'Sam Lee' }, []),
    ).toContain('Waiting for the claimant');
  });

  it('names the officer who owns a claim another officer cannot act on', () => {
    expect(waitingReason({ status: 'UNDER_REVIEW', assignedOfficerName: 'Sam Lee' }, [])).toContain(
      'Sam Lee',
    );
  });

  it('gives no reason when there is something to do', () => {
    expect(
      waitingReason({ status: 'SUBMITTED', assignedOfficerName: null }, ['PICK_UP']),
    ).toBeNull();
  });
});
