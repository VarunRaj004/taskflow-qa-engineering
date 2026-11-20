// Shared helpers for TaskFlow Playwright specs. Deterministic, synthetic test data only.
const { expect } = require('@playwright/test');

function uniqueEmail(prefix) {
  return `qa.${prefix}.${Date.now()}.${Math.floor(Math.random() * 100000)}@example.test`;
}

/**
 * Registers a fresh synthetic user through the UI, then logs in through the UI.
 * Returns { email, password, name }.
 */
async function registerAndLogin(page, prefix) {
  const email = uniqueEmail(prefix);
  const password = 'ValidPass123';
  const name = `QA ${prefix}`;

  await page.goto('/register');
  await page.getByLabel('Name').fill(name);
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Register' }).click();

  await page.waitForURL('**/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Log in' }).click();

  await page.waitForURL('**/dashboard');
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

  return { email, password, name };
}

async function getAuthToken(page) {
  return page.evaluate(() => localStorage.getItem('token'));
}

module.exports = { uniqueEmail, registerAndLogin, getAuthToken };
