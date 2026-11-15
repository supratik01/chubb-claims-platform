import { Browser, Page, expect } from '@playwright/test';

/** The password the demo seeder gives every user; see the README. */
export const DEMO_PASSWORD = process.env['E2E_DEMO_PASSWORD'] ?? 'Demo-Passw0rd!';

/** Signs in through the real sign-in form, using its labels, as a keyboard or screen-reader user would. */
export async function signIn(page: Page, username: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(DEMO_PASSWORD);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible();
}

/** A separate browser context per person, so the claimant and the officer are signed in at the same time. */
export async function newSession(browser: Browser, username: string): Promise<Page> {
  const page = await (await browser.newContext()).newPage();
  await signIn(page, username);
  return page;
}

/** Days from today as an ISO date, for form inputs. */
export function isoDate(daysFromToday: number): string {
  return new Date(Date.now() + daysFromToday * 86_400_000).toISOString().slice(0, 10);
}

/** Reports an incident as the signed-in claimant and returns the new claim's number. */
export async function reportIncident(
  page: Page,
  options: { policy: RegExp; description: string; amount: string },
): Promise<string> {
  await page
    .getByRole('navigation', { name: 'Main' })
    .getByRole('link', { name: 'Report an incident' })
    .click();
  // Wait for the policy list to load before choosing; reading the options straight away races the request.
  const option = page.getByLabel('Policy').locator('option').filter({ hasText: options.policy });
  await expect(option).toHaveCount(1);
  await page.getByLabel('Policy').selectOption((await option.getAttribute('value'))!);
  await page.getByLabel('Date of the incident').fill(isoDate(-2));
  await page.getByLabel('Where it happened (optional)').fill('Orchard Road, Singapore');
  await page.getByLabel('What happened').fill(options.description);
  await page.getByLabel(/Amount you are claiming/).fill(options.amount);
  await page.getByRole('button', { name: 'Submit claim' }).click();

  const heading = page.getByRole('heading', { level: 1, name: /^Claim CLM-/ });
  await expect(heading).toBeVisible();
  return (await heading.textContent())!.replace('Claim', '').trim();
}

/** Reads a currency total such as "SGD 22,700.00" from the exposure page as a number. */
export async function exposureTotal(page: Page, currency: string): Promise<number> {
  await page.goto('/staff/exposure');
  const card = page
    .getByRole('list', { name: 'Totals by currency' })
    .getByRole('listitem')
    .filter({ hasText: currency });
  const text = (await card.locator('.total__amount').textContent()) ?? '';
  return Number(text.replace(/[^0-9.]/g, ''));
}
