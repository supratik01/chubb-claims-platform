import AxeBuilder from '@axe-core/playwright';
import { Page, expect, test } from '@playwright/test';
import { signIn } from './support';

/**
 * Automated WCAG 2.1 A and AA checks (axe-core) on every main screen, in both colour schemes. Automated checks find
 * roughly a third of accessibility problems; they complement, not replace, the keyboard test below and manual review.
 */
async function expectNoViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();
  const summary = results.violations.map(
    (violation) =>
      `${violation.id}: ${violation.help} (${violation.nodes.map((node) => node.target).join(', ')})`,
  );
  expect(summary).toEqual([]);
}

for (const colorScheme of ['light', 'dark'] as const) {
  test.describe(`${colorScheme} theme`, () => {
    test.use({ colorScheme });

    test('the sign-in page has no detectable accessibility violations', async ({ page }) => {
      await page.goto('/login');
      await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();
      await expectNoViolations(page);
    });

    test('the claimant screens have no detectable accessibility violations', async ({ page }) => {
      await signIn(page, 'claimant.mei');
      await expect(page.getByRole('heading', { level: 1, name: 'My claims' })).toBeVisible();
      await expectNoViolations(page);

      await page.getByRole('link', { name: /^CLM-/ }).first().click();
      await expect(page.getByRole('heading', { level: 1, name: /^Claim CLM-/ })).toBeVisible();
      await expectNoViolations(page);

      await page.goto('/claimant/report');
      await expect(page.getByLabel('Policy').locator('option')).not.toHaveCount(1);
      await page.getByRole('button', { name: 'Submit claim' }).click();
      await expect(page.getByText('Describe what happened.')).toBeVisible();
      await expectNoViolations(page);
    });

    test('the staff screens have no detectable accessibility violations', async ({ page }) => {
      await signIn(page, 'manager.kenji');
      await page.goto('/staff/queue?view=all');
      await expect(page.getByRole('table')).toBeVisible();
      await expectNoViolations(page);

      await page
        .getByRole('row', { name: /Under review/ })
        .first()
        .getByRole('link')
        .click();
      await expect(page.getByRole('heading', { name: 'Actions' })).toBeVisible();
      await expectNoViolations(page);

      for (const path of ['/staff/workload', '/staff/exposure']) {
        await page.goto(path);
        await expect(page.getByRole('table')).toBeVisible();
        await expectNoViolations(page);
      }
    });
  });
}

test('the work queue can be used with the keyboard alone', async ({ page }) => {
  await signIn(page, 'officer.sam');
  await page.goto('/staff/queue');
  await expect(page.getByRole('table')).toBeVisible(); // Tab before the app renders would focus nothing useful

  // The skip link is the first stop and moves focus to the main region.
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: 'Skip to main content' })).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.locator('main')).toBeFocused();

  // Arrow keys move between the tabs, which changes the list.
  await page.getByRole('tab', { name: 'Unassigned' }).focus();
  await page.keyboard.press('ArrowRight');
  await expect(page.getByRole('tab', { name: 'My open claims' })).toBeFocused();
  await expect(page.getByRole('tab', { name: 'My open claims' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
  await expect(page).toHaveURL(/view=mine/);
});
