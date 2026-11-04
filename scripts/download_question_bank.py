#!/usr/bin/env python3
"""Download the public question-bank exports and assemble the app's offline JSON asset."""
import html
import json
import os
import re
import sys
from pathlib import Path
from urllib.parse import urljoin, urlparse, parse_qs, unquote

import requests

PAGE = "https://www.aishizhi.com/zh-CN/question-bank"
OUT = Path("app/src/main/assets/question_bank.json")
MIN_QUESTIONS = 3000

session = requests.Session()
session.headers.update({"User-Agent": "TVStudyGuardian-QuestionBankBuilder/1.0"})
response = session.get(PAGE, timeout=30)
response.raise_for_status()
page = html.unescape(response.text)

raw_urls = re.findall(
    r'(?:https?://www\.aishizhi\.com)?/api/question-bank-downloads/download\?[^"\'\s<>]+',
    page,
)
urls = list(dict.fromkeys(urljoin("https://www.aishizhi.com", u) for u in raw_urls))
if not urls:
    raise SystemExit("No question-bank download links found on the source page.")

all_questions = []
downloaded = 0
for url in urls:
    response = session.get(url, timeout=60)
    response.raise_for_status()
    data = response.json()
    questions = data if isinstance(data, list) else next(
        (data.get(k) for k in ("questions", "data", "items", "list")
         if isinstance(data.get(k), list)), []
    )
    if not questions:
        continue
    meta = data.get("meta", {}) if isinstance(data, dict) else {}
    for item in questions:
        if not isinstance(item, dict):
            continue
        q = dict(item)
        q.setdefault("grade", meta.get("grade", 1))
        if q.get("status", "published") != "published":
            continue
        if q.get("stem") and q.get("answer") is not None:
            all_questions.append(q)
    downloaded += 1
    print(f"Downloaded {downloaded}/{len(urls)} files; questions so far: {len(all_questions)}")

# De-duplicate by source question ID, preserving source order.
unique = {}
for q in all_questions:
    unique.setdefault(q.get("id") or (q.get("subject"), q.get("grade"), q.get("stem")), q)
all_questions = list(unique.values())

if len(all_questions) < MIN_QUESTIONS:
    raise SystemExit(
        f"Expected at least {MIN_QUESTIONS} published questions, got {len(all_questions)}. "
        "The source page/API may have changed; refusing to build with an incomplete bank."
    )

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(all_questions, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print(f"Offline question bank ready: {len(all_questions)} questions -> {OUT}")
