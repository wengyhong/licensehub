import { expect, test } from "@playwright/test";

test("user can register, create a product, and manage a license", async ({
  page,
}) => {
  const uniqueId = Date.now();
  const email = `e2e-${uniqueId}@example.com`;
  const customerEmail = `customer-${uniqueId}@example.com`;
  const password = "TestPassword123!";
  const productName = `E2E Product ${uniqueId}`;
  const updatedDescription = "Updated by the LicenseHub browser test.";

  await page.goto("/register");

  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Create account" }).click();

  await expect(page).toHaveURL(/\/login\?registered=true$/);

  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
  await expect(
    page.getByLabel(`Signed in as ${email}`),
  ).toBeVisible();

  await page.getByRole("link", { name: "Products", exact: true }).click();

  await expect(page).toHaveURL(/\/dashboard\/products$/);

  await page.getByRole("link", { name: "Create product" }).click();

  await expect(page).toHaveURL(/\/dashboard\/products\/new$/);
  await expect(
    page.getByRole("heading", { name: "Create product", exact: true }),
  ).toBeVisible();

  await page.getByLabel("Name").fill(productName);
  await page
    .getByLabel("Description")
    .fill("Created by the LicenseHub browser test.");

  await page.getByRole("button", { name: "Create product" }).click();

  await expect(page).toHaveURL(/\/dashboard\/products\/[0-9a-f-]+$/);
  await expect(page.getByRole("heading", { name: productName })).toBeVisible();

  await page.getByRole("link", { name: "Edit" }).click();

  await expect(page).toHaveURL(
    /\/dashboard\/products\/[0-9a-f-]+\/edit$/,
  );

  await page.getByLabel("Description").fill(updatedDescription);
  await page.getByRole("button", { name: "Save changes" }).click();

  await expect(page).toHaveURL(
    /\/dashboard\/products\/[0-9a-f-]+$/,
  );
  await expect(page.getByText(updatedDescription)).toBeVisible();

  await page
    .getByRole("link", { name: "Issue license" })
    .click();

  await expect(page).toHaveURL(
    /\/dashboard\/products\/[0-9a-f-]+\/licenses\/new$/,
  );
  await expect(
    page.getByRole("heading", { name: "Issue license", exact: true }),
  ).toBeVisible();

  await page.getByLabel("Customer email").fill(customerEmail);

  await page.getByLabel("Maximum activations").fill("2");

  const expirationInput = page.getByLabel("Expiration date");
  await expirationInput.fill("2030-01-01T12:00");
  await page.getByRole("button", { name: "Clear date" }).click();
  await expect(expirationInput).toHaveValue("");

  await page.getByRole("button", { name: "Issue license" }).click();

  const issuedLicense = page.getByRole("status");

  await expect(
    issuedLicense.getByRole("heading", {
      name: "Save this license key now",
    }),
  ).toBeVisible();
  await expect(page.getByLabel("Customer email")).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Issue license" }),
  ).toHaveCount(0);

  await expect(issuedLicense.locator("code")).toHaveText(
    /^LH_[A-F0-9]{16}_[A-Za-z0-9_-]{43}$/,
  );

  const fullLicenseKey = await issuedLicense.locator("code").innerText();

  await page
    .getByRole("link", { name: /Back to product/ })
    .click();

  await expect(page.getByText(customerEmail)).toBeVisible();

  await expect(page.getByText("ACTIVE", { exact: true })).toBeVisible();

  await expect(page.getByText("Maximum activations: 2")).toBeVisible();

  await page
    .getByRole("link", {
      name: /^License [A-F0-9]{16}$/,
    })
    .click();

  await expect(page).toHaveURL(/\/dashboard\/licenses\/[0-9a-f-]+$/);

  await expect(
    page.getByRole("heading", { name: "License details" }),
  ).toBeVisible();

  await expect(page.getByText(customerEmail)).toBeVisible();

  await expect(page.getByText(fullLicenseKey, { exact: true })).toHaveCount(0);

  await expect(page.getByText("No machine activations")).toBeVisible();

  page.once("dialog", async (dialog) => {
    expect(dialog.message()).toContain("Revoke this license?");
    await dialog.accept();
  });

  await page.getByRole("button", { name: "Revoke" }).click();

  await expect(page.getByText("REVOKED", { exact: true })).toBeVisible();

  await expect(page.getByRole("button", { name: "Revoke" })).toHaveCount(0);
});
