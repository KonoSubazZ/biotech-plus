/**
 * 首页图表配色：统一走仓库主题主色（themeStore.themeColors.primary）
 * <p>
 * 饼图这种多分类图用主色派生的同色系色阶，保证整页只有一个色相。
 */

/** 把 #rrggbb 与白色按比例混合（ratio 0 = 原色，1 = 纯白） */
export function mixWithWhite(hex: string, ratio: number) {
  const normalized = hex.replace('#', '');
  if (normalized.length !== 6) {
    return hex;
  }
  const channels = [0, 2, 4].map(offset => {
    const value = Number.parseInt(normalized.slice(offset, offset + 2), 16);
    const mixed = Math.round(value + (255 - value) * ratio);
    return mixed.toString(16).padStart(2, '0');
  });
  return `#${channels.join('')}`;
}

/** 主色 → 同色系色阶（饼图/多系列用），由深到浅 */
export function primaryPalette(primary: string, size: number) {
  return Array.from({ length: size }, (_, index) => mixWithWhite(primary, index * 0.15));
}
