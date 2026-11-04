/**
 * 扫描 public/iconify/*.json（前端离线图标包），生成菜单图标可选项与全量校验清单。
 *
 * 生成物：src/constants/iconify-icons.ts
 * 用法：node build/scripts/gen-iconify-manifest.mjs（或 pnpm gen:iconify）
 *
 * 为什么需要它：
 * .env 里 VITE_ICONIFY_URL=/iconify 说明图标只从本地离线包读取。
 * 若填了离线包里不存在的名字，请求会命中 vite 的 SPA fallback，
 * 返回 200 + index.html（text/html），iconify 解析失败后静默不渲染 —— 表现就是「改了图标不生效」。
 *
 * 菜单可选列表只收 ant-design：它是 1024x1024 网格 + 统一 padding，同族之间视觉尺寸本就接近。
 * 再乘上 build/scripts/iconify-scale.json 里的归一化系数，把各图标内容占比差异抹平
 * （原始占比 0.535~0.917，归一化后 0.723~0.820）。
 */
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = process.cwd();
const iconifyDir = path.join(root, 'public/iconify');
const outFile = path.join(root, 'src/constants/iconify-icons.ts');
const scaleFile = path.join(root, 'build/scripts/iconify-scale.json');

/** 该文件由本脚本生成，扫描时必须跳过，否则会被当成一个图标集合 */
const MANIFEST_NAME = 'manifest.json';

/**
 * 不进菜单可选列表的图标（json 里保留，供源码直接引用，只是不在菜单表单里出现）
 * 都是实心深色风格，与菜单要的浅色线条风格不一致
 */
const EXCLUDE_FROM_MENU = new Set([
  'material-symbols:account-circle-full',
  'material-symbols:nightlight-rounded',
  'material-symbols:sunny'
]);

/** 菜单可选列表只收 ant-design（同一套网格，视觉尺寸一致） */
const MENU_PREFIXES = new Set(['ant-design']);

/** 例外放行的单个图标：默认值 mdi:menu、业务明确要的备份图标 */
const MENU_EXTRA_ALLOW = new Set(['mdi:menu', 'material-symbols:backup-outline']);

if (!fs.existsSync(iconifyDir)) {
  console.error(`[gen-iconify] 目录不存在：${iconifyDir}`);
  process.exit(1);
}

const scaleMap = fs.existsSync(scaleFile) ? JSON.parse(fs.readFileSync(scaleFile, 'utf8')) : {};

const files = fs
  .readdirSync(iconifyDir)
  .filter(file => file.endsWith('.json') && file !== MANIFEST_NAME)
  .sort();

const allIcons = [];
const menuIcons = [];
let totalIcons = 0;

for (const file of files) {
  const fallbackPrefix = file.slice(0, -'.json'.length);
  let collection;

  try {
    collection = JSON.parse(fs.readFileSync(path.join(iconifyDir, file), 'utf8'));
  } catch (error) {
    console.error(`[gen-iconify] 解析失败，已跳过 ${file}：${error.message}`);
    continue;
  }

  const prefix = collection.prefix || fallbackPrefix;
  const names = Object.keys(collection.icons || {});
  if (!names.length) {
    console.warn(`[gen-iconify] ${file} 里没有 icons，已跳过`);
    continue;
  }
  totalIcons += names.length;

  for (const name of names) {
    const fullName = `${prefix}:${name}`;
    allIcons.push(fullName);

    if (EXCLUDE_FROM_MENU.has(fullName)) continue;
    if (!MENU_PREFIXES.has(prefix) && !MENU_EXTRA_ALLOW.has(fullName)) continue;

    menuIcons.push({ name: fullName, scale: scaleMap[fullName] ?? 1 });
  }
}

allIcons.sort();
menuIcons.sort((a, b) => a.name.localeCompare(b.name));

const menuLines = menuIcons
  .map(item => `  { name: '${item.name}', scale: ${item.scale} }`)
  .join(',\n');

const allLines = allIcons.map(name => `  '${name}'`).join(',\n');

const content = `/**
 * 本文件由 build/scripts/gen-iconify-manifest.mjs 自动生成，请勿手动修改。
 * 数据来源：public/iconify/*.json（${files.length} 个集合 / ${totalIcons} 个图标）
 *          + build/scripts/iconify-scale.json（视觉尺寸归一化系数）
 * 重新生成：pnpm gen:iconify
 */

export interface IconifyMenuItem {
  /** 图标全名，如 ant-design:home-outlined */
  name: string;
  /**
   * 视觉尺寸归一化系数。
   * 各图标内容在画布里占比不同（0.535~0.917），直接用同样尺寸渲染会显得有大有小；
   * 乘上这个系数后统一到 0.72~0.82，视觉尺寸一致。
   */
  scale: number;
}

/** 菜单图标可选列表（ant-design 同族，尺寸已归一化） */
export const ICONIFY_MENU_ICONS: IconifyMenuItem[] = [
${menuLines}
];

/** 离线包里所有图标，用于校验某个图标名到底能不能渲染 */
export const ICONIFY_ALL_ICONS: string[] = [
${allLines}
];
`;

fs.writeFileSync(outFile, content, 'utf8');
console.log(
  `[gen-iconify] ${files.length} 个集合 / ${totalIcons} 个图标｜菜单可选 ${menuIcons.length} 个 -> ${path.relative(root, outFile)}`
);
