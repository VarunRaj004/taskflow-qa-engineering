// @ts-check
const { test, expect } = require('@playwright/test');
const { registerAndLogin, getAuthToken } = require('./helpers');

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8090';

test.describe('Access control across users (REQ-ACCESS)', () => {
  test("a user cannot view or modify another user's task [REQ-ACCESS-001, 002, 003]", async ({ browser }) => {
    // Two fully separate browser contexts so each user's session is isolated, the same way
    // two different people using the app would be.
    const contextA = await browser.newContext();
    const contextB = await browser.newContext();
    const pageA = await contextA.newPage();
    const pageB = await contextB.newPage();

    await registerAndLogin(pageA, 'access-a');
    await registerAndLogin(pageB, 'access-b');

    // User B creates a task through the real UI.
    await pageB.goto('/tasks');
    await pageB.getByRole('button', { name: 'New Task' }).click();
    await pageB.getByLabel('Title').fill("User B private task");
    await pageB.getByRole('button', { name: 'Save Task' }).click();
    await expect(pageB.getByText('User B private task')).toBeVisible();

    // REQ-ACCESS-001: User A's own task list never shows User B's task.
    await pageA.goto('/tasks');
    await expect(pageA.getByText('User B private task')).toHaveCount(0);

    // Find the task's id via User B's own authenticated API call (not guessed), then use
    // User A's token to attempt to read/modify/delete it directly against the API -
    // this is the part of the journey the UI has no route for (there is no per-task URL to
    // navigate to), so it is verified at the API layer, exactly as a real attacker would have
    // to.
    const tokenB = await getAuthToken(pageB);
    const listResponse = await pageB.request.get(`${API_BASE_URL}/api/tasks`, {
      headers: { Authorization: `Bearer ${tokenB}` },
    });
    const { tasks } = await listResponse.json();
    const targetTask = tasks.find((t) => t.title === 'User B private task');
    expect(targetTask).toBeTruthy();

    const tokenA = await getAuthToken(pageA);

    const getAsA = await pageA.request.get(`${API_BASE_URL}/api/tasks/${targetTask.id}`, {
      headers: { Authorization: `Bearer ${tokenA}` },
    });
    expect(getAsA.status()).toBe(404); // REQ-ACCESS-002: identical to a non-existent id

    const updateAsA = await pageA.request.put(`${API_BASE_URL}/api/tasks/${targetTask.id}`, {
      headers: { Authorization: `Bearer ${tokenA}`, 'Content-Type': 'application/json' },
      data: { status: 'DONE' },
    });
    expect(updateAsA.status()).toBe(404); // REQ-ACCESS-003

    const deleteAsA = await pageA.request.delete(`${API_BASE_URL}/api/tasks/${targetTask.id}`, {
      headers: { Authorization: `Bearer ${tokenA}` },
    });
    expect(deleteAsA.status()).toBe(404); // REQ-ACCESS-003

    // The task genuinely survived User A's attempts - confirmed by User B still being able to
    // see it, through the real UI, not just a second API call.
    await pageB.reload();
    await expect(pageB.getByText('User B private task')).toBeVisible();

    await contextA.close();
    await contextB.close();
  });
});
