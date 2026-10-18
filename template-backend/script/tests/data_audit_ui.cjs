// 真实隔离服务上的浏览器验收，凭证仅通过进程环境传递。
// 预期 DATA_AUDIT_UI: PASS；涵盖查询、详情、失败重试、空结果、下拉与窄屏。
const assert = require('node:assert/strict');
const path = require('node:path');
const { chromium } = require(process.env.AUDIT_TEST_PLAYWRIGHT_PACKAGE);

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  let page;
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } });
    page = await context.newPage();
    await page.goto(process.env.AUDIT_QA_URL, { waitUntil: 'networkidle' });
    await page.locator('input[placeholder*="用户名"]').fill('admin');
    await page.locator('input[type="password"]').fill(process.env.AUDIT_QA_PASSWORD);
    await page.getByRole('button', { name: /登\s*录/, exact: true }).click();
    await page.waitForURL(url => !url.pathname.includes('login'), { timeout: 30000 });
    await page.goto(process.env.AUDIT_QA_URL + '/compliance/data-audit', { waitUntil: 'networkidle' });
    assert(await page.getByText('数据审计', { exact: true }).first().isVisible());
    await page.getByText('样本履历', { exact: true }).click();
    assert(await page.getByRole('radio', { name: '样本履历', exact: true }).isChecked());
    assert(await page.getByText('请输入完整样本编号后查询', { exact: true }).isVisible());
    await page.getByPlaceholder('完整样本编号', { exact: true }).fill(process.env.AUDIT_QA_SAMPLE);
    const query = page.waitForResponse(response => response.url().includes('/compliance/dataAudit/list')
      && response.url().includes('subbarcode='));
    await page.getByRole('button', { name: '搜索', exact: true }).click();
    const response = await query;
    const body = await response.json();
    assert(body.rows.length >= 3);
    assert(body.rows.every(row => row.subbarcode === process.env.AUDIT_QA_SAMPLE));
    await page.getByRole('button', { name: '查看详情', exact: true }).first().click();
    await page.getByText('审计详情', { exact: true }).waitFor();
    await page.locator('.n-collapse-item__header').first().click();
    assert(await page.getByText('变化字段', { exact: true }).first().isVisible());
    await page.screenshot({ path: path.resolve('target/audit-ui-detail.png'), fullPage: true, animations: 'disabled' });
    await page.keyboard.press('Escape');
    await page.getByText('审计详情', { exact: true }).waitFor({ state: 'hidden' });
    await context.setOffline(true);
    await page.getByRole('button', { name: '查看详情', exact: true }).first().click();
    await page.getByText('审计详情加载失败', { exact: true }).waitFor();
    await context.setOffline(false);
    await page.getByRole('button', { name: '重新加载', exact: true }).click();
    await page.locator('.n-collapse-item__header').first().waitFor();
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: '重置', exact: true }).click();
    assert(await page.getByText('请输入完整样本编号后查询', { exact: true }).isVisible());
    await page.getByText('变更记录', { exact: true }).click();
    await page.getByRole('button', { name: '搜索', exact: true }).waitFor({ state: 'visible' });
    await page.getByRole('button', { name: '搜索', exact: true }).click({ trial: true });
    await page.locator('.n-base-selection').first().click();
    await page.getByText('非样本对象', { exact: true }).last().waitFor();
    await page.keyboard.press('Escape');
    await page.setViewportSize({ width: 390, height: 844 });
    await page.waitForFunction(() => document.documentElement.scrollWidth <= window.innerWidth + 1);
    await page.screenshot({ path: path.resolve('target/audit-ui-mobile.png'), fullPage: true, animations: 'disabled' });
    assert(await page.getByRole('button', { name: '搜索', exact: true }).isVisible());
    await page.getByPlaceholder('完整样本编号', { exact: true }).fill('AUDIT-NO-RESULT');
    const empty = page.waitForResponse(response => response.url().includes('/compliance/dataAudit/list')
      && response.url().includes('AUDIT-NO-RESULT'));
    await page.getByRole('button', { name: '搜索', exact: true }).click();
    assert.equal((await (await empty).json()).rows.length, 0);
    console.log('DATA_AUDIT_UI: PASS (history, detail, retry, empty, select, keyboard, mobile)');
  } catch (error) {
    if (page) {
      await page.screenshot({ path: path.resolve('target/audit-ui-failure.png'), fullPage: true });
      console.error('VISIBLE_PAGE:', (await page.locator('body').innerText()).slice(0, 2200));
      console.error('OVERFLOW:', await page.evaluate(() => [...document.querySelectorAll('body *')]
        .filter(element => element.getBoundingClientRect().right > window.innerWidth + 2)
        .slice(0, 16).map(element => ({ tag: element.tagName, classes: element.className,
          width: element.getBoundingClientRect().width, right: element.getBoundingClientRect().right }))));
    }
    throw error;
  } finally {
    await browser.close();
  }
})().catch(error => { console.error('DATA_AUDIT_UI: FAIL', error.message); process.exitCode = 1; });
