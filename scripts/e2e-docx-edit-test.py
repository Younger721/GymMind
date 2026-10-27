#!/usr/bin/env python3
"""模拟 OnlyOffice 保存回调：改 docx → 重解析 → 发布 → 检索验证。"""
from __future__ import annotations

import base64
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from threading import Thread

BASE = "http://localhost:8080"
USERNAME = "admin"
PASSWORD = "123456"
DOC_ID = 14
MARKER = "GymMindE2E编辑测试20261013"
SERVE_PORT = 8765


def api(method: str, path: str, token: str | None = None, data: bytes | None = None, headers: dict | None = None):
    url = BASE + path
    h = dict(headers or {})
    if token:
        h["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    with urllib.request.urlopen(req, timeout=120) as resp:
        body = resp.read()
        if not body:
            return None
        return json.loads(body.decode("utf-8"))


def login() -> str:
    payload = json.dumps({"username": USERNAME, "password": PASSWORD}).encode("utf-8")
    r = api("POST", "/api/v1/auth/login", data=payload, headers={"Content-Type": "application/json"})
    return r["data"]["accessToken"]


def decode_onlyoffice_jwt(token: str) -> dict:
    parts = token.split(".")
    if len(parts) < 2:
        raise ValueError("invalid jwt")
    payload = parts[1]
    payload += "=" * (-len(payload) % 4)
    raw = base64.urlsafe_b64decode(payload.encode("ascii"))
    return json.loads(raw.decode("utf-8"))


def download_bytes(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=120) as resp:
        return resp.read()


def patch_docx(docx_bytes: bytes, marker: str) -> bytes:
    import io

    inp = io.BytesIO(docx_bytes)
    out = io.BytesIO()
    with zipfile.ZipFile(inp, "r") as zin, zipfile.ZipFile(out, "w") as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "word/document.xml":
                text = data.decode("utf-8")
                insert = (
                    f'<w:p><w:r><w:t>{marker}</w:t></w:r></w:p>'
                )
                if marker in text:
                    pass
                elif "</w:body>" in text:
                    text = text.replace("</w:body>", insert + "</w:body>", 1)
                else:
                    raise RuntimeError("unexpected document.xml")
                data = text.encode("utf-8")
            zout.writestr(item, data)
    return out.getvalue()


def start_file_server(directory: Path) -> ThreadingHTTPServer:
    class Handler(SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=str(directory), **kwargs)

    server = ThreadingHTTPServer(("127.0.0.1", SERVE_PORT), Handler)
    Thread(target=server.serve_forever, daemon=True).start()
    return server


def main() -> int:
    print("1. 登录…")
    token = login()
    print("2. 获取编辑器配置…")
    cfg_resp = api("GET", f"/api/v1/knowledge/documents/{DOC_ID}/editor-config", token=token)
    jwt = cfg_resp["data"]["token"]
    oo_cfg = decode_onlyoffice_jwt(jwt)
    doc_url = oo_cfg["document"]["url"]
    callback_url = oo_cfg["editorConfig"]["callbackUrl"]
    print("   document url:", doc_url[:80], "…")

    print("3. 下载原 docx…")
    original = download_bytes(doc_url)
    print(f"   大小 {len(original)} 字节")

    print("4. 注入测试段落…")
    modified = patch_docx(original, MARKER)
    tmp = Path(__file__).resolve().parent / "_e2e_docx_out"
    tmp.mkdir(exist_ok=True)
    out_file = tmp / "modified.docx"
    out_file.write_bytes(modified)

    print("5. 启动临时 HTTP 服务并模拟 OnlyOffice 回调…")
    server = start_file_server(tmp)
    file_url = f"http://127.0.0.1:{SERVE_PORT}/modified.docx"
    cb_body = json.dumps({"status": 2, "url": file_url, "key": oo_cfg["document"]["key"]}).encode("utf-8")
    cb_req = urllib.request.Request(
        callback_url,
        data=cb_body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(cb_req, timeout=120) as resp:
        cb_result = json.loads(resp.read().decode("utf-8"))
    server.shutdown()
    if cb_result.get("error") != 0:
        print("回调失败:", cb_result)
        return 1
    print("   回调成功")

    print("6. 等待重解析就绪（最多 3 分钟）…")
    for i in range(60):
        doc = api("GET", f"/api/v1/knowledge/documents/{DOC_ID}", token=token)
        status = doc["data"]["status"]
        published = doc["data"]["published"]
        print(f"   [{i+1}] status={status} published={published}")
        if status == "READY":
            break
        if status == "FAILED":
            print("解析失败")
            return 1
        time.sleep(3)
    else:
        print("超时未就绪")
        return 1

    if not doc["data"]["published"]:
        print("7. 发布文档…")
        api("PATCH", f"/api/v1/knowledge/documents/{DOC_ID}/publication?published=true", token=token)

    print("8. 知识检索…")
    q = urllib.parse.quote(MARKER)
    search = api("GET", f"/api/v1/knowledge/search?q={q}", token=token)
    hits = search.get("data") or []
    text_blob = json.dumps(hits, ensure_ascii=False)
    if MARKER in text_blob:
        print("✅ 检索命中测试内容，编辑→解析→发布→可用 链路通过")
        return 0
    print("⚠️ 未在检索结果中看到标记，hits:", text_blob[:500])
    return 2


if __name__ == "__main__":
    sys.exit(main())
