import { expect, test } from '@playwright/test';
import { DEMO_PASSWORD, signIn } from './support';

test('a claimant who opens a staff address is sent to their own claims', async ({ page }) => {
  await signIn(page, 'claimant.mei');
  await page.goto('/staff/queue');
  await expect(page).toHaveURL(/\/claimant$/);
  await expect(page.getByRole('heading', { level: 1, name: 'My claims' })).toBeVisible();
});

test('an officer has no exposure link and is sent back to the queue if they open it', async ({
  page,
}) => {
  await signIn(page, 'officer.sam');
  const nav = page.getByRole('navigation', { name: 'Main' });
  await expect(nav.getByRole('link', { name: 'Work queue' })).toBeVisible();
  await expect(nav.getByRole('link', { name: 'Exposure' })).toHaveCount(0);

  await page.goto('/staff/exposure');
  await expect(page).toHaveURL(/\/staff\/queue/);
});

test('a manager sees outstanding exposure per currency', async ({ page }) => {
  await signIn(page, 'manager.kenji');
  await page
    .getByRole('navigation', { name: 'Main' })
    .getByRole('link', { name: 'Exposure' })
    .click();
  await expect(page.getByRole('heading', { level: 1, name: 'Outstanding exposure' })).toBeVisible();
  await expect(
    page.getByRole('list', { name: 'Totals by currency' }).getByRole('listitem'),
  ).toHaveCount(2);
});

test('a wrong password is refused with a clear message', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Username').fill('claimant.mei');
  await page.getByLabel('Password').fill(`${DEMO_PASSWORD}-wrong`);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('alert')).toContainText('The username or password is not correct.');
});

test('a signed-out visitor is sent to sign in and returned to the page they asked for', async ({
  page,
}) => {
  await page.goto('/claimant/report');
  await expect(page).toHaveURL(/\/login\?returnUrl=%2Fclaimant%2Freport/);
  await page.getByLabel('Username').fill('claimant.mei');
  await page.getByLabel('Password').fill(DEMO_PASSWORD);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Report an incident' })).toBeVisible();
});
