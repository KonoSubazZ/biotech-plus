/**
 * 精简 public/iconify 离线图标包：只保留浅色单色线条风格 + 后台系统常用语义的图标。
 *
 * 数据源：node_modules/@iconify/json/json/*.json（devDependency，全量图标库）
 * 产物：public/iconify/{ant-design,mdi}.json（material-symbols.json 保持原样）
 *
 * 用法：node build/scripts/curate-iconify.mjs
 *
 * 设计口径：
 * - 主集合 ant-design outlined：2px 线条、单色（palette=false），与系统现有风格一致
 * - material-symbols：保留仓库源码里已实际使用的 21 个，不动
 * - mdi：只留默认图标 mdi:menu 等少量通用图标
 * - 删除品牌/彩色集合（logos、simple-icons、devicon、arcticons、cbi、codex、file-icons 等）
 * - 想增减图标，改下面的清单后重跑本脚本，再跑 gen-iconify-manifest.mjs
 */
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = process.cwd();
const sourceDir = path.join(root, 'node_modules/@iconify/json/json');
const targetDir = path.join(root, 'public/iconify');

/** ant-design outlined：按后台系统语义分组搭配，全部单色线条 */
const ANT_DESIGN = [
  // 导航 / 布局
  'home-outlined', 'menu-outlined', 'menu-fold-outlined', 'menu-unfold-outlined', 'layout-outlined',
  'appstore-outlined', 'appstore-add-outlined', 'dashboard-outlined', 'table-outlined', 'bars-outlined',
  'more-outlined', 'ellipsis-outlined', 'drag-outlined', 'pushpin-outlined', 'global-outlined',
  // 用户 / 权限
  'user-outlined', 'user-add-outlined', 'user-delete-outlined', 'user-switch-outlined',
  'usergroup-add-outlined', 'usergroup-delete-outlined', 'team-outlined', 'contacts-outlined',
  'idcard-outlined', 'profile-outlined', 'solution-outlined', 'safety-outlined',
  'safety-certificate-outlined', 'security-scan-outlined', 'audit-outlined', 'key-outlined',
  'lock-outlined', 'unlock-outlined', 'login-outlined', 'logout-outlined', 'control-outlined',
  'verified-outlined',
  // 文件 / 文档
  'file-outlined', 'file-add-outlined', 'file-done-outlined', 'file-text-outlined',
  'file-search-outlined', 'file-sync-outlined', 'file-zip-outlined', 'file-excel-outlined',
  'file-word-outlined', 'file-ppt-outlined', 'file-pdf-outlined', 'file-image-outlined',
  'file-unknown-outlined', 'file-exclamation-outlined', 'file-protect-outlined', 'folder-outlined',
  'folder-add-outlined', 'folder-open-outlined', 'folder-view-outlined', 'paper-clip-outlined',
  'snippets-outlined', 'book-outlined', 'read-outlined', 'form-outlined', 'project-outlined',
  'copy-outlined', 'save-outlined', 'printer-outlined', 'scan-outlined', 'qrcode-outlined',
  'barcode-outlined',
  // 数据 / 报表
  'bar-chart-outlined', 'line-chart-outlined', 'pie-chart-outlined', 'area-chart-outlined',
  'dot-chart-outlined', 'radar-chart-outlined', 'funnel-plot-outlined', 'heat-map-outlined',
  'fund-view-outlined', 'database-outlined', 'hdd-outlined', 'cloud-server-outlined',
  'monitor-outlined', 'desktop-outlined', 'laptop-outlined', 'tablet-outlined', 'mobile-outlined',
  'api-outlined', 'code-outlined', 'console-sql-outlined', 'function-outlined',
  // 常用操作
  'plus-outlined', 'plus-circle-outlined', 'plus-square-outlined', 'minus-outlined',
  'minus-circle-outlined', 'minus-square-outlined', 'close-outlined', 'close-circle-outlined',
  'close-square-outlined', 'check-outlined', 'check-circle-outlined', 'check-square-outlined',
  'delete-outlined', 'edit-outlined', 'search-outlined', 'zoom-in-outlined', 'zoom-out-outlined',
  'filter-outlined', 'sort-ascending-outlined', 'sort-descending-outlined', 'swap-outlined',
  'sync-outlined', 'reload-outlined', 'redo-outlined', 'undo-outlined', 'rollback-outlined',
  'rotate-left-outlined', 'rotate-right-outlined', 'export-outlined', 'import-outlined',
  'download-outlined', 'upload-outlined', 'cloud-download-outlined', 'cloud-upload-outlined',
  'cloud-sync-outlined', 'send-outlined', 'share-alt-outlined', 'link-outlined', 'merge-outlined',
  'branches-outlined', 'clear-outlined',
  // 状态 / 提示
  'info-outlined', 'info-circle-outlined', 'question-outlined', 'question-circle-outlined',
  'exclamation-outlined', 'exclamation-circle-outlined', 'warning-outlined', 'alert-outlined',
  'loading-outlined', 'clock-circle-outlined', 'hourglass-outlined', 'schedule-outlined',
  'calendar-outlined', 'history-outlined', 'exception-outlined', 'stop-outlined',
  'pause-circle-outlined', 'play-circle-outlined', 'step-forward-outlined', 'step-backward-outlined',
  'fast-forward-outlined', 'fast-backward-outlined', 'caret-down-outlined', 'caret-left-outlined',
  'caret-right-outlined', 'caret-up-outlined',
  // 方向
  'arrow-down-outlined', 'arrow-left-outlined', 'arrow-right-outlined', 'arrow-up-outlined',
  'double-left-outlined', 'double-right-outlined', 'to-top-outlined', 'enter-outlined',
  // 通讯 / 通知
  'mail-outlined', 'message-outlined', 'bell-outlined', 'notification-outlined', 'comment-outlined',
  'customer-service-outlined', 'inbox-outlined', 'phone-outlined', 'video-camera-outlined',
  'video-camera-add-outlined',
  // 环境 / 系统
  'environment-outlined', 'setting-outlined', 'flag-outlined', 'tag-outlined', 'tags-outlined',
  'star-outlined',
  // 业务 / 工具（生物信息与数据管理相关）
  'experiment-outlined', 'medicine-box-outlined', 'bug-outlined', 'build-outlined', 'tool-outlined',
  'cluster-outlined', 'deployment-unit-outlined', 'container-outlined', 'gateway-outlined',
  'robot-outlined', 'sliders-outlined', 'switcher-outlined', 'interaction-outlined',
  'translation-outlined', 'percentage-outlined', 'number-outlined', 'compass-outlined',
  'block-outlined', 'insert-row-above-outlined', 'merge-cells-outlined',
  // 仓库原本就在用的（重建时必须保留，否则已配置的菜单会掉图标）
  'column-width-outlined', 'line-outlined', 'money-collect-outlined', 'trademark-circle-outlined'
];

/** material-symbols 在原有基础上补的业务图标（原有的一律保留不动） */
const MATERIAL_EXTRA = ['backup-outline'];

function pick(sourceName, names) {
  const file = path.join(sourceDir, `${sourceName}.json`);
  if (!fs.existsSync(file)) {
    console.error(`[curate-iconify] 缺少源文件 ${file}，请先安装 devDependencies`);
    process.exit(1);
  }
  const raw = JSON.parse(fs.readFileSync(file, 'utf8'));
  const icons = {};
  const missing = [];

  for (const name of names) {
    const icon = raw.icons?.[name];
    if (!icon) {
      missing.push(name);
      continue;
    }
    icons[name] = typeof icon === 'string' ? { body: icon } : icon;
  }

  if (missing.length) {
    console.warn(`[curate-iconify] ${sourceName}: ${missing.length} 个名字不存在，已跳过 -> ${missing.join(', ')}`);
  }

  return {
    payload: { prefix: raw.prefix || sourceName, width: raw.width, height: raw.height, icons },
    count: Object.keys(icons).length
  };
}

const antDesign = pick('ant-design', ANT_DESIGN);
fs.writeFileSync(path.join(targetDir, 'ant-design.json'), `${JSON.stringify(antDesign.payload)}\n`, 'utf8');
console.log(`[curate-iconify] ant-design.json        ${antDesign.count} 个图标`);

// material-symbols：保留文件里现有的全部图标，只补 MATERIAL_EXTRA
const msFile = path.join(targetDir, 'material-symbols.json');
const msCurrent = JSON.parse(fs.readFileSync(msFile, 'utf8'));
const msSource = JSON.parse(fs.readFileSync(path.join(sourceDir, 'material-symbols.json'), 'utf8'));
const msAdded = [];
for (const name of MATERIAL_EXTRA) {
  if (msCurrent.icons[name]) continue;
  const icon = msSource.icons?.[name];
  if (!icon) {
    console.warn(`[curate-iconify] material-symbols 里没有 ${name}，已跳过`);
    continue;
  }
  msCurrent.icons[name] = typeof icon === 'string' ? { body: icon } : icon;
  msAdded.push(name);
}
if (msAdded.length) {
  fs.writeFileSync(msFile, `${JSON.stringify(msCurrent)}\n`, 'utf8');
  console.log(`[curate-iconify] material-symbols.json   新增 ${msAdded.join(', ')}（现有 ${Object.keys(msCurrent.icons).length} 个）`);
} else {
  console.log(`[curate-iconify] material-symbols.json   无变化（${Object.keys(msCurrent.icons).length} 个）`);
}

console.log('[curate-iconify] 其它集合一律不动（系统各处可能直接引用，删了会掉图标）');

