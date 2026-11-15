import { navigationFor } from './navigation';

describe('navigationFor', () => {
  const labels = (role: Parameters<typeof navigationFor>[0]) =>
    navigationFor(role).map((link) => link.label);

  it('gives claimants their claims and the incident form only', () => {
    expect(labels('CLAIMANT')).toEqual(['My claims', 'Report an incident']);
  });

  it('gives officers the queue and workload but not exposure', () => {
    expect(labels('CLAIMS_OFFICER')).toEqual(['Work queue', 'Team workload']);
  });

  it('gives managers everything officers have, plus exposure', () => {
    expect(labels('CLAIMS_MANAGER')).toEqual(['Work queue', 'Team workload', 'Exposure']);
  });
});
