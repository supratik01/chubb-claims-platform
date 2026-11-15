import { expect, test } from '@playwright/test';
import { exposureTotal, newSession, reportIncident } from './support';

/**
 * The main demo flow, end to end across both micro-frontends, the API and Kafka:
 * a claimant reports an incident; an officer picks it up and asks a question; the claimant answers; the officer
 * approves; the claimant sees the decision; the manager's exposure figures follow.
 */
test('a claim goes from report to approval, and the claimant and manager see each step', async ({
  browser,
}) => {
  const claimant = await newSession(browser, 'claimant.mei');
  const officer = await newSession(browser, 'officer.sam');
  const manager = await newSession(browser, 'manager.kenji');
  const sgdBefore = await exposureTotal(manager, 'SGD');

  // The claimant reports an incident and lands on the new claim.
  const claimNumber = await reportIncident(claimant, {
    policy: /POL-SG-M-10021/,
    description: `Hit by a cyclist while parked; front door dented. (${Date.now()})`,
    amount: '1000',
  });
  await expect(claimant.getByText('We have received your claim.')).toBeVisible();

  // The exposure read model is built from Kafka events, so the new reserve arrives asynchronously.
  await expect.poll(() => exposureTotal(manager, 'SGD')).toBe(sgdBefore + 1000);

  // The officer finds it in the unassigned queue and picks it up.
  await officer.goto('/staff/queue');
  await officer.getByRole('button', { name: `Pick up claim ${claimNumber}` }).click();
  await expect(
    officer.getByRole('heading', { level: 1, name: `Claim ${claimNumber}` }),
  ).toBeVisible();
  await expect(officer.getByText('Under review').first()).toBeVisible();

  // The officer asks for more information.
  await officer
    .getByLabel('Question for the claimant')
    .fill('Do you have the cyclist’s contact details?');
  await officer.getByRole('button', { name: 'Send question' }).click();
  await expect(
    officer.getByText('Waiting for the claimant to answer the open question.'),
  ).toBeVisible();

  // The claimant's open page refreshes in the background and asks for an answer without a reload.
  await expect(claimant.getByText('We need more information from you.')).toBeVisible();
  await claimant.getByLabel('Your answer').fill('Yes: Daniel Lim, 9123 4567.');
  await claimant.getByRole('button', { name: 'Send answer' }).click();
  await expect(claimant.getByText('Answer: Yes: Daniel Lim, 9123 4567.')).toBeVisible();

  // Back with the officer, the answer is visible and the claim can be decided: approve less than claimed.
  await officer.reload();
  await expect(officer.getByText('Answer: Yes: Daniel Lim, 9123 4567.')).toBeVisible();
  await officer.getByLabel(/Approved amount/).fill('800');
  await officer.getByLabel('Note for the claimant (optional)').fill('Approved less the excess.');
  await officer.getByRole('button', { name: 'Approve claim' }).click();
  await expect(officer.getByRole('button', { name: 'Record payment' })).toBeVisible();

  // The claimant sees the decision.
  await expect(claimant.getByRole('heading', { name: 'Decision: approved' })).toBeVisible();
  await expect(claimant.getByText(/SGD\s800\.00/).first()).toBeVisible();
  await expect(claimant.getByText('Approved less the excess.').first()).toBeVisible();

  // The reserve dropped from the claimed 1,000 to the approved 800.
  await expect.poll(() => exposureTotal(manager, 'SGD')).toBe(sgdBefore + 800);

  // Recording the payment closes the claim and removes it from exposure.
  await officer.getByRole('button', { name: 'Record payment' }).click();
  await expect(officer.getByText('This claim has been paid and is closed.')).toBeVisible();
  await expect.poll(() => exposureTotal(manager, 'SGD')).toBe(sgdBefore);

  // The officer's closed count in the team workload includes it.
  await manager.goto('/staff/workload');
  await expect(manager.getByRole('row', { name: /Sam Lee/ })).toBeVisible();
});

test('an officer rejects a claim with a reason, and the claimant sees why', async ({ browser }) => {
  const claimant = await newSession(browser, 'claimant.jack');
  const officer = await newSession(browser, 'officer.priya');

  const claimNumber = await reportIncident(claimant, {
    policy: /POL-AU-M-30388/,
    description: `Paint faded on the bonnet over several years. (${Date.now()})`,
    amount: '600',
  });

  await officer.goto('/staff/queue');
  await officer.getByRole('button', { name: `Pick up claim ${claimNumber}` }).click();
  await officer.getByRole('radio', { name: 'Reject' }).check();
  await officer.getByRole('button', { name: 'Reject claim' }).click();
  await expect(officer.getByText('Give the reason for rejecting this claim.')).toBeVisible();

  await officer
    .getByLabel('Reason for rejection')
    .fill('Gradual deterioration is excluded under section 4.2.');
  await officer.getByRole('button', { name: 'Reject claim' }).click();
  await expect(officer.getByText('This claim was rejected and is closed.')).toBeVisible();

  await expect(claimant.getByRole('heading', { name: 'Decision: not approved' })).toBeVisible();
  await expect(
    claimant.getByText('Gradual deterioration is excluded under section 4.2.').first(),
  ).toBeVisible();
});
