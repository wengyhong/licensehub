import { expect, test } from "@playwright/test";

test("user can register, create a product, and manage a license", async ({
  page,
}) => {
  const uniqueId = Date.now();
  const email = `e2e-${uniqueId}@example.com`;
  const customerEmail = `customer-${uniqueId}@example.com`;
  const password = "TestPassword123!";
  const productName = `E2E Product ${uniqueId}`;

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

  await page.getByRole("link", { name: "Products", exact: true }).click();

  await expect(page).toHaveURL(/\/dashboard\/products$/);

  await page.getByLabel("Name").fill(productName);
  await page
    .getByLabel("Description")
    .fill("Created by the LicenseHub browser test.");

  await page.getByRole("button", { name: "Create product" }).click();

  await expect(page.getByRole("link", { name: productName })).toBeVisible();

  await page.getByRole("link", { name: productName }).click();

  await expect(page.getByRole("heading", { name: productName })).toBeVisible();

  await page.getByLabel("Customer email").fill(customerEmail);

  await page.getByLabel("Maximum activations").fill("2");

  await page.getByRole("button", { name: "Issue license" }).click();

  const issuedLicense = page.getByRole("status");

  await expect(
    issuedLicense.getByRole("heading", {
      name: "Save this license key now",
    }),
  ).toBeVisible();

  await expect(issuedLicense.locator("code")).toHaveText(
    /^LH_[A-F0-9]{16}_[A-Za-z0-9_-]{43}$/,
  );

  await expect(page.getByText(customerEmail)).toBeVisible();

  await expect(page.getByText("ACTIVE", { exact: true })).toBeVisible();

  await expect(page.getByText("Maximum activations: 2")).toBeVisible();

  const fullLicenseKey = await issuedLicense.locator("code").innerText();

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
