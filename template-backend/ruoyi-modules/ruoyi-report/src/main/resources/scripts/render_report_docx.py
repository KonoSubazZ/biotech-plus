#!/usr/bin/env python3
"""读一个 UTF-8 JSON 对象，用 docxtpl 渲染 DOCX 模板；不做任何业务预处理。

边界（对齐设计文档 docs/context/report/report-docx-renderer.md）：
- Java 负责查库、组装 JSON、决定路径；本脚本只做渲染与体检；
- 退出码：0 成功 / 2 输入错 / 3 模板或渲染错 / 4 写输出错；
- stdout 一行 JSON（成功结果），stderr 一行 JSON（错误），供 Java 解析。

调用：python render_report_docx.py <template.docx> <input.json> <output.docx>
Java 侧调用方：org.dromara.report.service.ReportDocxRenderer（ProcessBuilder 参数数组，
不拼 shell 字符串；stdout+stderr 重定向到日志文件，避免管道死锁）。
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import re
import sys
import tempfile
import time
import zipfile
from pathlib import Path
from typing import Any

from docxtpl import DocxTemplate
from jinja2 import Environment, StrictUndefined
from jinja2.exceptions import TemplateError


class RendererError(Exception):
    """Base exception with a stable CLI exit code."""

    exit_code = 1


class InputValidationError(RendererError):
    exit_code = 2


class TemplateRenderError(RendererError):
    exit_code = 3


class OutputWriteError(RendererError):
    exit_code = 4


_JINJA_MARKER = re.compile(r"(?:{{|{%|{#).*?(?:}}|%}|#})", re.DOTALL)


def load_json_object(json_path: Path) -> dict[str, Any]:
    """Load UTF-8 JSON and require an object at the root."""
    if not json_path.is_file():
        raise InputValidationError(f"JSON文件不存在: {json_path}")
    try:
        with json_path.open("r", encoding="utf-8") as stream:
            value = json.load(stream)
    except UnicodeDecodeError as exc:
        raise InputValidationError(f"JSON必须使用UTF-8编码: {json_path}") from exc
    except json.JSONDecodeError as exc:
        raise InputValidationError(
            f"JSON格式错误: line={exc.lineno}, column={exc.colno}, message={exc.msg}"
        ) from exc
    if not isinstance(value, dict):
        raise InputValidationError("JSON根节点必须是对象")
    return value


def _load_template(template_path: Path) -> tuple[DocxTemplate, bytes]:
    if not template_path.is_file():
        raise InputValidationError(f"模板文件不存在: {template_path}")
    if template_path.suffix.lower() != ".docx":
        raise InputValidationError("模板文件必须是.docx")
    try:
        # 内存副本避免并发渲染时污染项目内模板文件。
        template_bytes = template_path.read_bytes()
        return DocxTemplate(io.BytesIO(template_bytes)), template_bytes
    except (OSError, zipfile.BadZipFile) as exc:
        raise InputValidationError(f"无法读取DOCX模板: {template_path}") from exc


def _find_unrendered_markers(document_bytes: bytes) -> list[str]:
    markers: set[str] = set()
    try:
        with zipfile.ZipFile(io.BytesIO(document_bytes)) as archive:
            broken_part = archive.testzip()
            if broken_part:
                raise TemplateRenderError(f"生成的DOCX压缩包损坏: {broken_part}")
            for name in archive.namelist():
                if not name.startswith("word/") or not name.endswith(".xml"):
                    continue
                text = archive.read(name).decode("utf-8", errors="ignore")
                markers.update(match.group(0) for match in _JINJA_MARKER.finditer(text))
    except zipfile.BadZipFile as exc:
        raise TemplateRenderError("生成结果不是有效DOCX文件") from exc
    return sorted(markers)


def _contains_template_markup(part_name: str, data: bytes) -> bool:
    if not part_name.startswith("word/") or not part_name.endswith(".xml"):
        return False
    # Word 可能把一个 Jinja 标签拆到多个XML标签之间，先去掉标签再判断。
    text_only = re.sub(rb"<[^>]+>", b"", data)
    return any(token in text_only for token in (b"{{", b"{%", b"{#"))


def _restore_unrendered_package_parts(template_bytes: bytes, rendered_bytes: bytes) -> bytes:
    """Keep rendered XML only for parts that contained template markup."""
    with zipfile.ZipFile(io.BytesIO(template_bytes)) as source:
        source_entries = {
            item.filename: (item, source.read(item.filename)) for item in source.infolist()
        }
    with zipfile.ZipFile(io.BytesIO(rendered_bytes)) as rendered:
        rendered_entries = [
            (item, rendered.read(item.filename)) for item in rendered.infolist()
        ]

    template_parts = {
        name
        for name, (_, data) in source_entries.items()
        if _contains_template_markup(name, data)
    }
    merged = io.BytesIO()
    written: set[str] = set()
    with zipfile.ZipFile(merged, "w") as target:
        for rendered_info, rendered_data in rendered_entries:
            name = rendered_info.filename
            if name in source_entries and name not in template_parts:
                source_info, source_data = source_entries[name]
                target.writestr(source_info, source_data)
            else:
                target.writestr(rendered_info, rendered_data)
            written.add(name)
        for name, (source_info, source_data) in source_entries.items():
            if name not in written and name not in template_parts:
                target.writestr(source_info, source_data)
    return merged.getvalue()


def render_to_bytes(template_path: Path, context: dict[str, Any]) -> tuple[bytes, list[str]]:
    """Render to memory and return bytes plus referenced top-level variables."""
    template, template_bytes = _load_template(template_path)
    environment = Environment(undefined=StrictUndefined, autoescape=True)
    try:
        referenced_variables = sorted(
            template.get_undeclared_template_variables(jinja_env=environment)
        )
        missing = sorted(set(referenced_variables) - set(context))
        if missing:
            raise TemplateRenderError("JSON缺少模板顶层字段: " + ", ".join(missing))

        template.render(context, jinja_env=environment, autoescape=True)
        output = io.BytesIO()
        template.save(output)
        document_bytes = _restore_unrendered_package_parts(
            template_bytes, output.getvalue()
        )
    except RendererError:
        raise
    except TemplateError as exc:
        raise TemplateRenderError(f"模板或JSON字段错误: {exc}") from exc
    except Exception as exc:
        raise TemplateRenderError(f"DOCX渲染失败: {exc}") from exc

    markers = _find_unrendered_markers(document_bytes)
    if markers:
        preview = ", ".join(markers[:5])
        raise TemplateRenderError(f"生成结果仍包含未渲染模板标记: {preview}")
    return document_bytes, referenced_variables


def render_report(
    template_path: Path,
    json_path: Path,
    output_path: Path | None,
    *,
    validate_only: bool = False,
    overwrite: bool = True,
) -> dict[str, Any]:
    """Validate or atomically render one JSON object into a DOCX template."""
    template_path = template_path.expanduser().resolve()
    json_path = json_path.expanduser().resolve()
    context = load_json_object(json_path)

    if not validate_only and output_path is None:
        raise InputValidationError("生成模式必须提供输出DOCX路径")

    resolved_output: Path | None = None
    if output_path is not None:
        resolved_output = output_path.expanduser().resolve()
        if resolved_output.suffix.lower() != ".docx":
            raise InputValidationError("输出文件必须是.docx")
        if resolved_output == template_path:
            raise InputValidationError("输出路径不能覆盖模板文件")
        if not overwrite and resolved_output.exists():
            raise InputValidationError(f"输出文件已存在: {resolved_output}")

    started = time.perf_counter()
    document_bytes, referenced_variables = render_to_bytes(template_path, context)

    if resolved_output is not None and not validate_only:
        resolved_output.parent.mkdir(parents=True, exist_ok=True)
        temporary_path: Path | None = None
        try:
            with tempfile.NamedTemporaryFile(
                mode="wb",
                prefix=f".{resolved_output.name}.",
                suffix=".tmp",
                dir=resolved_output.parent,
                delete=False,
            ) as stream:
                stream.write(document_bytes)
                stream.flush()
                os.fsync(stream.fileno())
                temporary_path = Path(stream.name)
            os.replace(temporary_path, resolved_output)
            temporary_path = None
        except OSError as exc:
            raise OutputWriteError(f"写入DOCX失败: {resolved_output}") from exc
        finally:
            if temporary_path is not None:
                temporary_path.unlink(missing_ok=True)

    result: dict[str, Any] = {
        "status": "VALID" if validate_only else "GENERATED",
        "templatePath": str(template_path),
        "jsonPath": str(json_path),
        "jsonTopLevelFields": sorted(context),
        "templateVariables": referenced_variables,
        "sha256": hashlib.sha256(document_bytes).hexdigest().upper(),
        "elapsedMs": round((time.perf_counter() - started) * 1000),
    }
    if resolved_output is not None and not validate_only:
        result["outputPath"] = str(resolved_output)
        result["outputBytes"] = len(document_bytes)
    return result


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="读取JSON对象并使用docxtpl渲染DOCX模板，不执行数据库或业务规则。"
    )
    parser.add_argument("template", type=Path, help="DOCX模板路径")
    parser.add_argument("json", type=Path, help="UTF-8 JSON文件路径")
    parser.add_argument("output", type=Path, nargs="?", help="输出DOCX路径")
    parser.add_argument(
        "--validate-only",
        action="store_true",
        help="完整渲染到内存并校验，但不写输出文件",
    )
    parser.add_argument(
        "--no-overwrite",
        action="store_true",
        help="输出文件已存在时失败",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        result = render_report(
            args.template,
            args.json,
            args.output,
            validate_only=args.validate_only,
            overwrite=not args.no_overwrite,
        )
        print(json.dumps(result, ensure_ascii=False, separators=(",", ":")))
        return 0
    except RendererError as exc:
        error = {
            "status": "ERROR",
            "errorType": exc.__class__.__name__,
            "message": str(exc),
        }
        print(json.dumps(error, ensure_ascii=False, separators=(",", ":")), file=sys.stderr)
        return exc.exit_code


if __name__ == "__main__":
    raise SystemExit(main())
