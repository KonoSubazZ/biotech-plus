import { ICONIFY_ALL_ICONS, ICONIFY_MENU_ICONS } from '@/constants/iconify-icons';
import type { IconifyMenuItem } from '@/constants/iconify-icons';

/**
 * 菜单可选的 iconify 图标（ant-design 同族，带视觉尺寸归一化系数）
 *
 * 只收 public/iconify 离线包里真实存在的图标：离线包外的名字不会被渲染，
 * 所以菜单表单只从这里取值，避免填出显示不出来的图标。
 */
export function getIconifyMenuIcons(): IconifyMenuItem[] {
  return ICONIFY_MENU_ICONS;
}

/**
 * 该图标名是否在离线包里存在（决定了能不能渲染出来）
 *
 * 注意与上方可选列表区分：可选列表做了风格和尺寸筛选，
 * 而系统别处可能直接引用了不在可选列表里、但在离线包里存在的老图标。
 */
export function isIconifyIconAvailable(name: string) {
  return ICONIFY_ALL_ICONS.includes(name);
}
