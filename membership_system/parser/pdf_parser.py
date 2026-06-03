import os

os.environ["FLAGS_use_mkldnn"] = "0"
os.environ["FLAGS_enable_pir_api"] = "0"

from paddleocr import PaddleOCR

_ocr_model = None


def get_ocr_model():
    global _ocr_model

    if _ocr_model is None:
        _ocr_model = PaddleOCR(
            lang="ch",
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False
        )

    return _ocr_model

import re
import json
from pathlib import Path

import fitz
from PIL import Image


def render_first_page(pdf_path, zoom=2):
    doc = fitz.open(str(pdf_path))
    page = doc[0]
    matrix = fitz.Matrix(zoom, zoom)
    pix = page.get_pixmap(matrix=matrix, alpha=False)
    img_path = pdf_path.with_suffix(".page1.png")
    pix.save(str(img_path))
    return Image.open(img_path)


def extract_pdf_text_by_blocks(pdf_path):
    doc = fitz.open(str(pdf_path))
    all_lines = []

    for page_index, page in enumerate(doc):
        blocks = page.get_text("dict")["blocks"]

        for block in blocks:
            if block.get("type") != 0:
                continue

            for line in block.get("lines", []):
                text = "".join(
                    span.get("text", "")
                    for span in line.get("spans", [])
                ).strip()

                if text:
                    x0, y0, x1, y1 = line["bbox"]
                    all_lines.append({
                        "page": page_index + 1,
                        "x": round(x0, 2),
                        "y": round(y0, 2),
                        "text": text
                    })

    all_lines.sort(key=lambda item: (item["page"], item["y"], item["x"]))
    return all_lines


import numpy as np

ocr_model = PaddleOCR(
    lang="ch",
    use_doc_orientation_classify=False,
    use_doc_unwarping=False,
    use_textline_orientation=False
)


def ocr_crop(image, box):
    crop = image.crop(box).convert("RGB")
    img_array = np.array(crop)

    result = get_ocr_model().predict(img_array)

    lines = []

    for res in result:
        data = None

        if isinstance(res, dict):
            data = res
        elif hasattr(res, "json"):
            data = res.json
            if callable(data):
                data = data()
        elif hasattr(res, "to_dict"):
            data = res.to_dict()

        if not data:
            continue

        texts = data.get("rec_texts", [])
        scores = data.get("rec_scores", [])

        for text, score in zip(texts, scores):
            if score >= 0.5:
                lines.append(text)

    return normalize_text("\n".join(lines))


def normalize_text(text):
    text = text.replace("Al", "AI")
    text = text.replace("电子邮箱;", "电子邮箱:")
    text = text.replace("到岗时间;", "到岗时间:")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{2,}", "\n", text)
    return text.strip()


def extract_basic_info(text):
    result = {
        "姓名": None,
        "年龄": None,
        "求职意向": None,
        "到岗时间": None,
        "联系电话": None,
        "电子邮箱": None
    }

    phone = re.search(r"1[3-9]\d{9}", text)
    email = re.search(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", text)
    age = re.search(r"年龄[:：]\s*(\d{1,2})\s*岁?", text)
    intention = re.search(r"意向[:：]\s*([A-Za-z\u4e00-\u9fa5 ]+开发)", text)
    arrival = re.search(r"到岗时间[:：]\s*([一二三四五六七八九十0-9]+周到岗|随时到岗|月内到岗)", text)

    name_candidates = re.findall(r"[\u4e00-\u9fa5]{2,4}", text)
    blacklist = {"年龄", "意向", "应用开发", "到岗时间", "联系电话", "电子邮箱", "一周到岗"}
    for item in name_candidates:
        if item not in blacklist and not any(key in item for key in blacklist):
            result["姓名"] = item
            break

    if age:
        result["年龄"] = age.group(1) + "岁"
    if intention:
        result["求职意向"] = intention.group(1).strip()
    if arrival:
        result["到岗时间"] = arrival.group(1).strip()
    if phone:
        result["联系电话"] = phone.group(0)
    if email:
        result["电子邮箱"] = email.group(0)

    return result


def extract_education(text):
    education = []

    pattern = re.compile(
        r"(?P<start>\d{4}年\d{1,2}月)\s*[-至]\s*(?P<end>\d{4}年\d{1,2}月)\s+"
        r"(?P<school>[\u4e00-\u9fa5]+大学|[\u4e00-\u9fa5]+学院)"
        r"\s*(?P<tag>[\u4e00-\u9fa5A-Za-z0-9 ]*)\s+"
        r"(?P<major>[\u4e00-\u9fa5A-Za-z与]+)[-—](?P<degree>[\u4e00-\u9fa5]+)"
    )

    for match in pattern.finditer(text):
        item = match.groupdict()
        education.append({
            "时间": item["start"] + "至" + item["end"],
            "学校": item["school"],
            "补充信息": item["tag"].strip(),
            "专业": item["major"],
            "学历": item["degree"]
        })

    return education


def extract_skills(text):
    skills = {}

    for key in ["开发语言", "数据库", "中间件", "框架"]:
        match = re.search(
            key + r"[:：]\s*(.*?)(?=\n.*?[:：]|智能烹饪问答|铁路购票系统|$)",
            text,
            re.S
        )
        if match:
            value = re.sub(r"\s+", " ", match.group(1)).strip(" 。")
            skills[key] = value

    return skills


def extract_projects(text):
    projects = []

    header_pattern = re.compile(
        r"(?P<name>智能烹饪问答与知识检索\s*Agent\s*系统|铁路购票系统项目)\s+"
        r"(?P<role>大模型应用开发|后端开发)\s+"
        r"(?P<time>\d{4}\.\d{2}-\d{4}\.\d{2})"
    )

    headers = list(header_pattern.finditer(text))

    for index, header in enumerate(headers):
        start = header.end()
        end = headers[index + 1].start() if index + 1 < len(headers) else len(text)
        body = text[start:end]

        desc = re.search(r"项目描述[:：]\s*(.*?)(?=主要技术栈[:：])", body, re.S)
        stack = re.search(r"主要技术栈[:：]\s*(.*?)(?=项目内容[:：])", body, re.S)
        content = re.search(r"项目内容[:：]\s*(.*)", body, re.S)

        duties = []
        if content:
            raw_items = re.split(r"\n?\s*\d+\.\s*", content.group(1))
            duties = [
                re.sub(r"\s+", " ", item).strip(" 。；")
                for item in raw_items
                if item.strip()
            ]

        projects.append({
            "项目名称": re.sub(r"\s+", " ", header.group("name")).strip(),
            "项目角色": header.group("role"),
            "项目时间": header.group("time"),
            "项目描述": re.sub(r"\s+", " ", desc.group(1)).strip() if desc else None,
            "主要技术栈": re.sub(r"\s+", " ", stack.group(1)).strip() if stack else None,
            "项目内容": duties
        })

    return projects


def parse_resume(pdf_file):
    pdf_path = Path(pdf_file)

    block_lines = extract_pdf_text_by_blocks(pdf_path)
    native_text = "\n".join(item["text"] for item in block_lines)
    native_text = normalize_text(native_text)

    image = render_first_page(pdf_path)

    width, height = image.size

    top_text = ocr_crop(
        image,
        box=(0, 40, width, int(height * 0.18))
    )

    education_text = ocr_crop(
        image,
        box=(40, int(height * 0.10), width - 40, int(height * 0.27))
    )

    merged_text = normalize_text(top_text + "\n" + education_text + "\n" + native_text)

    return {
        "基本信息": extract_basic_info(top_text),
        "教育背景": extract_education(education_text),
        "专业技能": extract_skills(native_text),
        "项目经历": extract_projects(native_text),
        "原始合并文本": merged_text
    }


if __name__ == "__main__":
    result = parse_resume("../uploads/简历.pdf")
    print(json.dumps(result, ensure_ascii=False, indent=2))
