// @ts-check
const { test, expect } = require('@playwright/test');
const { registerAndLogin } = require('./helpers');

test.describe('Task management (REQ-TASK)', () => {
  test.beforeEach(async ({ page }) => {
    await registerAndLogin(page, 'tasks');
    await page.goto('/tasks');
    await expect(page.getByRole('heading', { name: 'Tasks' })).toBeVisible();
  });

  test('an authenticated user can create a task [REQ-TASK-001]', async ({ page }) => {
    await page.getByRole('button', { name: 'New Task' }).click();
    await page.getByLabel('Title').fill('Buy milk');
    await page.getByLabel('Description').fill('2% milk, the big carton');
    await page.getByRole('button', { name: 'Save Task' }).click();

    await expect(page.getByText('Buy milk')).toBeVisible();
    await expect(page.getByText('2% milk, the big carton')).toBeVisible();
  });

  test('creating a task with a blank title is rejected [REQ-TASK-002]', async ({ page }) => {
    await page.getByRole('button', { name: 'New Task' }).click();
    await page.getByLabel('Title').fill('   ');
    await page.getByRole('button', { name: 'Save Task' }).click();

    // Form should still be open with an inline error, not silently accepted.
    await expect(page.getByRole('alert')).toBeVisible();
  });

  test('an authenticated user can update a task, and it survives a refresh [REQ-TASK-010, REQ-TASK-013]', async ({ page }) => {
    await page.getByRole('button', { name: 'New Task' }).click();
    await page.getByLabel('Title').fill('Write report');
    await page.getByRole('button', { name: 'Save Task' }).click();
    await expect(page.getByText('Write report')).toBeVisible();

    await page.getByRole('button', { name: 'Edit task: Write report' }).click();
    await page.getByLabel('Title').fill('Write final report');
    await page.getByRole('button', { name: 'Save Task' }).click();

    await expect(page.getByText('Write final report')).toBeVisible();
    await expect(page.getByText('Write report', { exact: true })).toHaveCount(0);

    await page.reload();
    await expect(page.getByText('Write final report')).toBeVisible();
  });

  test('task status can be changed [REQ-TASK-010]', async ({ page }) => {
    await page.getByRole('button', { name: 'New Task' }).click();
    await page.getByLabel('Title').fill('Deploy release');
    await page.getByRole('button', { name: 'Save Task' }).click();
    await expect(page.getByText('Deploy release')).toBeVisible();

    const row = page.locator('li.task-row', { hasText: 'Deploy release' });
    await expect(row.getByText('Status: TODO')).toBeVisible();

    await page.getByRole('button', { name: 'Edit task: Deploy release' }).click();
    await page.locator('form.task-form').getByLabel('Status').selectOption('DONE');
    await page.getByRole('button', { name: 'Save Task' }).click();

    await expect(row.getByText('Status: DONE')).toBeVisible();
  });

  test('an authenticated user can delete a task [REQ-TASK-011]', async ({ page }) => {
    await page.getByRole('button', { name: 'New Task' }).click();
    await page.getByLabel('Title').fill('Temporary task');
    await page.getByRole('button', { name: 'Save Task' }).click();
    await expect(page.getByText('Temporary task')).toBeVisible();

    await page.getByRole('button', { name: 'Delete task: Temporary task' }).click();
    await expect(page.getByText('Temporary task')).toHaveCount(0);

    await page.reload();
    await expect(page.getByText('Temporary task')).toHaveCount(0);
  });

  test('an empty / no-match task list shows a clear empty state, not an error [REQ-TASK-014]', async ({ page }) => {
    await page.getByLabel('Search').fill('no-task-will-ever-match-this-string-xyz');
    await expect(page.locator('li.task-row')).toHaveCount(0);
    await expect(page.getByRole('alert')).toHaveCount(0);
  });
});
