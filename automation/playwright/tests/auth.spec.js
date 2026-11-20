// @ts-check
const { test, expect } = require('@playwright/test');
const { registerAndLogin, uniqueEmail } = require('./helpers');

test.describe('Authentication (REQ-AUTH)', () => {
  test('a user can register and then log in with valid credentials [REQ-AUTH-001, REQ-AUTH-004]', async ({ page }) => {
    const email = uniqueEmail('login-ok');
    const password = 'ValidPass123';

    await page.goto('/register');
    await page.getByLabel('Name').fill('QA Login Check');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: 'Register' }).click();
    await page.waitForURL('**/login');

    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: 'Log in' }).click();

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Log out' })).toBeVisible();
  });

  test('invalid login is rejected with an understandable message [REQ-AUTH-005]', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill(uniqueEmail('no-such-user'));
    await page.getByLabel('Password').fill('WhateverPassword123');
    await page.getByRole('button', { name: 'Log in' }).click();

    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('alert')).toContainText(/invalid|incorrect|email or password/i);
  });

  test('protected pages redirect an unauthenticated visitor to /login [REQ-AUTH-009]', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);

    await page.goto('/tasks');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('log out ends the session and returns to a logged-out state [REQ-AUTH-010]', async ({ page }) => {
    await registerAndLogin(page, 'logout-check');

    await page.getByRole('button', { name: 'Log out' }).click();
    await expect(page).toHaveURL(/\/login$/);

    // The session is really gone, not just the UI state: revisiting a protected route
    // redirects again rather than showing cached content.
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
  });
});
