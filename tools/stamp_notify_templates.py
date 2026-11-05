#!/usr/bin/env python3
"""给两份 SOP 通知模板打 docxtpl 占位符（可重复执行）。

背景：`~/project/templates/notify/` 下的两份正式模板是 Word 里新做的空白表单，
没有任何 `{{ }}` 占位符；生成链（docxtpl）需要占位符才能渲染。

做法：只改表格里对应的值单元格，保留单元格原有字体/字号（复制首个 run 的 rPr）；
重复执行结果一致。执行前会 dump 一份 `.orig-YYYYMMDD.docx` 备份（若不存在）。

用法：
  python3 tools/stamp_notify_templates.py            # 就地写两份模板
  python3 tools/stamp_notify_templates.py --dry-run  # 只打印将要写入的内容
"""
from __future__ import annotations

import copy
import shutil
import sys
from datetime import date
from pathlib import Path

from docx import Document
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.table import _Cell
from docx.text.paragraph import Paragraph

TEMPLATE_DIR = Path("/home/liushangzhi/project/templates/notify")

QC_FILE = "样本质控不合格通知单.docx"
DELAY_FILE = "检测报告延迟通知单.docx"

HEADER_CELLS = {
    (0, 1): "{{ studyNo }}",
    (0, 4): "{{ projectNo }}",
    (1, 1): "{{ participantNo }}",
    (1, 4): "{{ sampleNo }}",
    (2, 1): "{{ center }}",
    (2, 4): "{{ receiveDate }}",
    (3, 1): "{{ sampleType }}",
    (3, 4): "{{ sampleCount }}",
    (4, 1): "{{ product }}",
}

QC_CELLS = {
    (5, 1): (
        "{{ box(stageReceive) }} 样本接收    {{ box(stagePathology) }} 病理质控    "
        "{{ box(stageNucleic) }} 核酸质控    {{ box(stageLibrary) }} 文库质控    "
        "{{ box(stageSequencing) }} 测序质控"
    ),
    (7, 1): "{{ box(terminateYes) }} 是\n{{ box(terminatePause) }} 暂停相关检测，待申办方确认后处理",
    (8, 1): (
        "{{ box(remedyResample) }} 重新采集符合要求的样本重新寄送\n"
        "{{ box(remedyWaxBlock) }} 更换肿瘤含量较高的蜡块重新切片后寄送\n"
        "{{ box(remedyMoreSlices) }} 选取组织面积较大或较多的切片重新寄送\n"
        "{{ box(remedyResend) }} 重新送样\n"
        "{{ box(remedyOther) }} 其他：{{ remedyOtherText or '________________________' }}"
    ),
}

DELAY_CELLS = {
    (5, 1): "{{ delayReason }}",
    (6, 1): "{{ expectedReportDate }}",
}

# 「不合格原因」单元格要保留原有的参考文案，只在最前面插一行占位符
QC_PREPEND = {(6, 1): "不合格原因：{{ reason }}"}


def _first_rpr(cell: _Cell):
    """取单元格里第一个带 rPr 的 run 的字符属性（用来保住字体/字号）。"""
    for paragraph in cell.paragraphs:
        for run in paragraph.runs:
            if run._r.rPr is not None:
                return copy.deepcopy(run._r.rPr)
    return None


def _first_ppr(cell: _Cell):
    p = cell.paragraphs[0]._p
    return copy.deepcopy(p.pPr) if p.pPr is not None else None


def _new_paragraph(cell: _Cell, text: str, ppr, rpr) -> Paragraph:
    p = OxmlElement("w:p")
    if ppr is not None:
        p.append(copy.deepcopy(ppr))
    tc = cell._tc
    tc.append(p)
    paragraph = Paragraph(p, cell)
    run = paragraph.add_run(text)
    if rpr is not None:
        run._r.insert(0, copy.deepcopy(rpr))
    return paragraph


def set_cell_lines(cell: _Cell, text: str) -> None:
    """把单元格内容换成 text（\\n 分段），保留首段段落属性与首个 run 的字符属性。"""
    lines = text.split("\n")
    ppr = _first_ppr(cell)
    rpr = _first_rpr(cell)

    # 删掉除第一段之外的所有段落
    for paragraph in cell.paragraphs[1:]:
        paragraph._p.getparent().remove(paragraph._p)

    first = cell.paragraphs[0]
    for run in list(first.runs):
        run._r.getparent().remove(run._r)
    run = first.add_run(lines[0])
    if rpr is not None:
        run._r.insert(0, copy.deepcopy(rpr))

    for line in lines[1:]:
        _new_paragraph(cell, line, ppr, rpr)


def prepend_line(cell: _Cell, text: str) -> None:
    """在单元格最前面插一行（保留原有内容；已存在则不重复插）。"""
    if cell.paragraphs and cell.paragraphs[0].text.strip().startswith(text.split("{{")[0].strip()):
        return
    ppr = _first_ppr(cell)
    rpr = _first_rpr(cell)
    _new_paragraph_at(cell, text, ppr, rpr, 1)
    _new_paragraph_at(cell, "", ppr, rpr, 2)


def _new_paragraph_at(cell: _Cell, text: str, ppr, rpr, index: int) -> None:
    p = OxmlElement("w:p")
    if ppr is not None:
        p.append(copy.deepcopy(ppr))
    cell._tc.insert(index, p)
    if text:
        paragraph = Paragraph(p, cell)
        run = paragraph.add_run(text)
        if rpr is not None:
            run._r.insert(0, copy.deepcopy(rpr))


def stamp(path: Path, cells: dict[tuple[int, int], str], dry_run: bool,
          prepend: dict[tuple[int, int], str] | None = None) -> None:
    doc = Document(str(path))
    table = doc.tables[0]
    for (row_idx, col_idx), text in cells.items():
        cell = table.rows[row_idx].cells[col_idx]
        if dry_run:
            print(f"  r{row_idx}c{col_idx}: {text[:70]!r}")
            continue
        set_cell_lines(cell, text)
    for (row_idx, col_idx), text in (prepend or {}).items():
        cell = table.rows[row_idx].cells[col_idx]
        if dry_run:
            print(f"  r{row_idx}c{col_idx} (prepend): {text!r}")
            continue
        prepend_line(cell, text)
    if not dry_run:
        doc.save(str(path))


def ensure_backup(path: Path) -> None:
    backup = path.with_name(f"{path.stem}.orig-{date.today():%Y%m%d}.docx")
    if not backup.exists():
        shutil.copy2(path, backup)
        print(f"  backup -> {backup.name}")


def main() -> int:
    dry = "--dry-run" in sys.argv
    jobs = (
        (QC_FILE, {**HEADER_CELLS, **QC_CELLS}, QC_PREPEND),
        (DELAY_FILE, {**HEADER_CELLS, **DELAY_CELLS}, {}),
    )
    for name, cells, prepend in jobs:
        path = TEMPLATE_DIR / name
        if not path.exists():
            print(f"!! 模板不存在：{path}")
            return 1
        print(f"== {name}")
        if not dry:
            ensure_backup(path)
        stamp(path, cells, dry, prepend)
    print("done" + (" (dry-run)" if dry else ""))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
