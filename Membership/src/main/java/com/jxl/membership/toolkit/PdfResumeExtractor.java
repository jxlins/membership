package com.jxl.membership.toolkit;

import com.jxl.membership.common.constant.MemberConstants;
import com.jxl.membership.dto.ResumeParsedData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfResumeExtractor {

    private static final int MIN_TEXT_LENGTH = 30;
    private static final int MAX_BIO_LENGTH = 200;
    private static final int MAX_ACHIEVEMENT_LENGTH = 800;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?\\d[\\d\\s\\-()]{7,}\\d)");

    private static final Pattern ORCID_PATTERN =
            Pattern.compile("\\b\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X]\\b");

    private static final Pattern URL_PATTERN =
            Pattern.compile("(https?://[^\\s，。；;]+|www\\.[^\\s，。；;]+)");

    private static final Pattern CHINESE_NAME_PATTERN =
            Pattern.compile("^[\\u4e00-\\u9fa5·]{2,8}$");

    private static final Pattern ENGLISH_NAME_PATTERN =
            Pattern.compile("^[A-Z][a-zA-Z.\\-]+\\s+[A-Z][a-zA-Z.\\-]+(\\s+[A-Z][a-zA-Z.\\-]+)?$");

    public ResumeExtractResult extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            return extract(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("PDF简历解析失败：" + e.getMessage(), e);
        }
    }

    public ResumeExtractResult extract(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("PDF文件不存在");
        }

        try (PDDocument document = PDDocument.load(file)) {
            return extractFromDocument(document);
        } catch (Exception e) {
            throw new RuntimeException("PDF简历解析失败：" + e.getMessage(), e);
        }
    }

    public ResumeExtractResult extract(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            return extractFromDocument(document);
        } catch (Exception e) {
            throw new RuntimeException("PDF简历解析失败：" + e.getMessage(), e);
        }
    }

    private ResumeExtractResult extractFromDocument(PDDocument document) throws Exception {
        if (document == null || document.getNumberOfPages() == 0) {
            throw new IllegalArgumentException("PDF内容为空");
        }

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setAddMoreFormatting(true);
        stripper.setStartPage(1);
        stripper.setEndPage(document.getNumberOfPages());

        String rawText = stripper.getText(document);
        String text = normalizeText(rawText);

        ResumeExtractResult result = new ResumeExtractResult();
        result.setExtractedText(text);

        if (text.length() < MIN_TEXT_LENGTH) {
            result.setParseStatus(MemberConstants.RESUME_PARSE_FAILED);
            result.setMessage("PDF中可抽取文本过少，可能是扫描版PDF，当前代码不包含OCR");
            result.setParsedData(new ResumeParsedData());
            result.setConfidence(emptyConfidence());
            return result;
        }

        List<String> lines = splitUsefulLines(text);
        List<String> firstPageLines = extractFirstPageLines(document);

        ResumeParsedData data = new ResumeParsedData();

        data.setEmail(extractBestEmail(text));
        data.setPhone(extractBestPhone(text));
        data.setOrcid(extractFirst(ORCID_PATTERN, text));
        data.setHomepage(extractBestHomepage(text));

        data.setMemberName(extractName(lines, firstPageLines, data.getEmail()));
        data.setOrganization(extractOrganization(lines));
        data.setDepartment(extractDepartment(lines));
        data.setPositionTitle(extractPositionTitle(lines));
        data.setHighestDegree(extractHighestDegree(text));
        data.setCountryRegion(extractCountryRegion(lines));
        data.setProfessionalField(extractProfessionalField(lines));
        data.setResearchDirection(extractResearchDirection(lines));
        data.setRepresentativeAchievements(extractRepresentativeAchievements(lines));
        data.setPersonalBio(generateBio(data));

        Map<String, String> confidence = buildConfidence(data);

        result.setParseStatus(MemberConstants.RESUME_PARSE_SUCCESS);
        result.setMessage("PDF简历解析成功，请人工核对后使用");
        result.setParsedData(data);
        result.setConfidence(confidence);

        return result;
    }

    private List<String> extractFirstPageLines(PDDocument document) {
        try {
            PDFTextStripper firstPageStripper = new PDFTextStripper();
            firstPageStripper.setSortByPosition(true);
            firstPageStripper.setAddMoreFormatting(true);
            firstPageStripper.setStartPage(1);
            firstPageStripper.setEndPage(1);

            String firstPageText = normalizeText(firstPageStripper.getText(document));
            return splitUsefulLines(firstPageText);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r", "\n")
                .replace("\u00A0", " ")
                .replace("：", ":")
                .replace("–", "-")
                .replace("—", "-")
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n[ ]+", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private List<String> splitUsefulLines(String text) {
        List<String> result = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        String[] lines = text.split("\\n");

        for (String line : lines) {
            String cleaned = cleanLine(line);

            if (cleaned.isEmpty()) {
                continue;
            }

            if (cleaned.length() > 260) {
                continue;
            }

            result.add(cleaned);
        }

        return result;
    }

    private String cleanLine(String line) {
        if (line == null) {
            return "";
        }

        return line
                .replaceAll("\\s+", " ")
                .replaceAll("^[•·●▪■◆◇\\-]+", "")
                .trim();
    }

    private String extractBestEmail(String text) {
        List<String> emails = extractAll(EMAIL_PATTERN, text);

        if (emails.isEmpty()) {
            return "";
        }

        for (String email : emails) {
            String lower = email.toLowerCase();
            if (!lower.endsWith(".png")
                    && !lower.endsWith(".jpg")
                    && !lower.contains("example")
                    && !lower.contains("xxx")) {
                return email;
            }
        }

        return emails.get(0);
    }

    private String extractBestPhone(String text) {
        List<String> phones = extractAll(PHONE_PATTERN, text);

        if (phones.isEmpty()) {
            return "";
        }

        for (String phone : phones) {
            String normalized = phone.replaceAll("[^0-9+]", "");

            if (normalized.length() >= 8 && normalized.length() <= 18) {
                return phone.trim();
            }
        }

        return phones.get(0).trim();
    }

    private String extractBestHomepage(String text) {
        List<String> urls = extractAll(URL_PATTERN, text);

        if (urls.isEmpty()) {
            return "";
        }

        for (String url : urls) {
            String lower = url.toLowerCase();

            if (lower.contains("scholar.google")
                    || lower.contains("github")
                    || lower.contains("linkedin")
                    || lower.contains("researchgate")
                    || lower.contains("orcid")
                    || lower.contains("homepage")
                    || lower.contains("personal")) {
                return trimUrl(url);
            }
        }

        return trimUrl(urls.get(0));
    }

    private String trimUrl(String url) {
        if (url == null) {
            return "";
        }

        return url
                .replaceAll("[,，。.;；]+$", "")
                .trim();
    }

    private String extractName(List<String> lines, List<String> firstPageLines, String email) {
        List<String> candidates = new ArrayList<>();

        int firstLimit = Math.min(firstPageLines.size(), 12);
        for (int i = 0; i < firstLimit; i++) {
            String line = firstPageLines.get(i);
            addNameCandidate(candidates, line);
        }

        int globalLimit = Math.min(lines.size(), 20);
        for (int i = 0; i < globalLimit; i++) {
            String line = lines.get(i);
            addNameCandidate(candidates, line);
        }

        if (email != null && email.contains("@")) {
            String prefix = email.substring(0, email.indexOf("@"));
            String guessedFromEmail = guessNameFromEmail(prefix);

            if (!guessedFromEmail.isEmpty()) {
                candidates.add(guessedFromEmail);
            }
        }

        if (candidates.isEmpty()) {
            return "";
        }

        return candidates.get(0);
    }

    private void addNameCandidate(List<String> candidates, String line) {
        if (line == null) {
            return;
        }

        String value = line.trim();

        if (value.length() > 60) {
            return;
        }

        String lower = value.toLowerCase();

        if (containsAny(lower,
                "resume", "curriculum vitae", "cv", "email", "phone", "mobile",
                "education", "experience", "publication", "research", "profile",
                "address", "homepage", "orcid", "个人简历", "简历", "邮箱",
                "电话", "教育", "经历", "论文", "研究方向")) {
            return;
        }

        String cleaned = value
                .replaceAll("^(name|姓名)[:：]", "")
                .trim();

        if (CHINESE_NAME_PATTERN.matcher(cleaned).matches()) {
            candidates.add(cleaned);
            return;
        }

        if (ENGLISH_NAME_PATTERN.matcher(cleaned).matches()) {
            candidates.add(cleaned);
        }
    }

    private String guessNameFromEmail(String prefix) {
        if (prefix == null) {
            return "";
        }

        String value = prefix
                .replaceAll("[0-9_\\-.]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (value.length() < 4 || value.length() > 40) {
            return "";
        }

        String[] parts = value.split(" ");
        if (parts.length < 2 || parts.length > 3) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.length() < 2) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }

        return builder.toString();
    }

    private String extractOrganization(List<String> lines) {
        List<ScoredText> candidates = new ArrayList<>();

        for (String line : lines) {
            int score = 0;
            String lower = line.toLowerCase();

            if (containsAny(lower, "university", "college", "institute", "academy")) {
                score += 5;
            }

            if (containsAny(lower, "school", "faculty", "department", "laboratory", "lab", "center", "centre")) {
                score += 3;
            }

            if (containsAny(line, "大学", "学院", "研究院", "实验室", "中心", "学部", "研究所")) {
                score += 5;
            }

            if (containsAny(lower, "education", "experience", "publication", "award", "skill")) {
                score -= 3;
            }

            if (line.length() > 160) {
                score -= 2;
            }

            if (score > 0) {
                candidates.add(new ScoredText(line, score));
            }
        }

        return best(candidates);
    }

    private String extractDepartment(List<String> lines) {
        List<ScoredText> candidates = new ArrayList<>();

        for (String line : lines) {
            int score = 0;
            String lower = line.toLowerCase();

            if (containsAny(lower, "department", "faculty", "school of", "college of")) {
                score += 5;
            }

            if (containsAny(line, "院系", "学部", "学院", "系", "部门", "专业委员会")) {
                score += 5;
            }

            if (containsAny(lower, "university", "institute")) {
                score += 1;
            }

            if (line.length() > 150) {
                score -= 2;
            }

            if (score > 0) {
                candidates.add(new ScoredText(line, score));
            }
        }

        return best(candidates);
    }

    private String extractPositionTitle(List<String> lines) {
        List<String> keywords = Arrays.asList(
                "professor", "associate professor", "assistant professor", "lecturer",
                "researcher", "engineer", "scientist", "director", "chair",
                "phd student", "doctoral student", "master student", "postdoctoral",
                "教授", "副教授", "讲师", "研究员", "副研究员", "工程师",
                "博士生", "硕士生", "博士后", "主任", "理事", "秘书长"
        );

        for (String line : lines) {
            String lower = line.toLowerCase();

            for (String keyword : keywords) {
                if (lower.contains(keyword.toLowerCase()) || line.contains(keyword)) {
                    return line;
                }
            }
        }

        return "";
    }

    private String extractHighestDegree(String text) {
        String lower = text.toLowerCase();

        int phdScore = count(lower, "ph.d") + count(lower, "phd") + count(text, "博士");
        int masterScore = count(lower, "master") + count(lower, "m.s.") + count(lower, "msc") + count(text, "硕士");
        int bachelorScore = count(lower, "bachelor") + count(lower, "b.s.") + count(text, "本科") + count(text, "学士");

        if (phdScore > 0) {
            return "博士";
        }

        if (masterScore > 0) {
            return "硕士";
        }

        if (bachelorScore > 0) {
            return "本科";
        }

        return "";
    }

    private String extractCountryRegion(List<String> lines) {
        List<String> countries = Arrays.asList(
                "China", "Singapore", "Malaysia", "Japan", "Korea", "United States",
                "USA", "UK", "United Kingdom", "Australia", "Canada",
                "中国", "新加坡", "马来西亚", "日本", "韩国", "美国", "英国",
                "澳大利亚", "加拿大"
        );

        for (String line : lines) {
            for (String country : countries) {
                if (line.contains(country)) {
                    return country;
                }
            }
        }

        return "";
    }

    private String extractProfessionalField(List<String> lines) {
        for (String line : lines) {
            String lower = line.toLowerCase();

            if (containsAny(lower, "major", "discipline", "field", "specialization")
                    || containsAny(line, "专业", "学科", "领域", "方向")) {
                String value = afterColon(line);
                return value.isEmpty() ? line : value;
            }
        }

        return "";
    }

    private String extractResearchDirection(List<String> lines) {
        List<String> sectionTitles = Arrays.asList(
                "research interests", "research interest", "research direction",
                "research areas", "research area", "research topics",
                "研究方向", "研究兴趣", "研究领域", "主要研究"
        );

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase();

            for (String title : sectionTitles) {
                if (lower.contains(title.toLowerCase()) || line.contains(title)) {
                    String inline = afterColon(line);

                    if (!inline.isEmpty() && inline.length() >= 2) {
                        return inline;
                    }

                    return collectSectionContent(lines, i + 1, 4);
                }
            }
        }

        List<String> fallbackKeywords = Arrays.asList(
                "artificial intelligence", "machine learning", "learning analytics",
                "educational technology", "data mining", "deep learning",
                "人工智能", "机器学习", "学习分析", "教育技术", "数据挖掘",
                "智慧教育", "智能教育", "大模型", "生成式人工智能"
        );

        LinkedHashSet<String> hits = new LinkedHashSet<>();

        for (String line : lines) {
            String lower = line.toLowerCase();

            for (String keyword : fallbackKeywords) {
                if (lower.contains(keyword.toLowerCase()) || line.contains(keyword)) {
                    hits.add(keyword);
                }
            }
        }

        if (hits.isEmpty()) {
            return "";
        }

        return String.join("，", hits);
    }

    private String extractRepresentativeAchievements(List<String> lines) {
        List<String> titles = Arrays.asList(
                "publications", "publication", "selected publications",
                "papers", "projects", "awards", "honors",
                "论文", "发表论文", "代表成果", "科研项目", "项目经历", "奖励", "荣誉"
        );

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase();

            for (String title : titles) {
                if (lower.contains(title.toLowerCase()) || line.contains(title)) {
                    return limitLength(collectSectionContent(lines, i + 1, 8), MAX_ACHIEVEMENT_LENGTH);
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        for (String line : lines) {
            String lower = line.toLowerCase();

            if (containsAny(lower, "journal", "conference", "publication", "paper", "project", "award")
                    || containsAny(line, "期刊", "会议", "论文", "项目", "奖励", "基金")) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }

                builder.append(line);

                if (builder.length() >= MAX_ACHIEVEMENT_LENGTH) {
                    break;
                }
            }
        }

        return limitLength(builder.toString(), MAX_ACHIEVEMENT_LENGTH);
    }

    private String collectSectionContent(List<String> lines, int start, int maxLines) {
        StringBuilder builder = new StringBuilder();
        int count = 0;

        for (int i = start; i < lines.size() && count < maxLines; i++) {
            String line = lines.get(i);

            if (isLikelySectionTitle(line) && count > 0) {
                break;
            }

            if (line.length() < 2) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append("，");
            }

            builder.append(line);
            count++;
        }

        return builder.toString();
    }

    private boolean isLikelySectionTitle(String line) {
        if (line == null) {
            return false;
        }

        String lower = line.toLowerCase();

        if (line.length() <= 30 && containsAny(lower,
                "education", "experience", "publication", "project", "award",
                "skill", "research", "profile", "contact")) {
            return true;
        }

        return line.length() <= 12 && containsAny(line,
                "教育经历", "工作经历", "项目经历", "论文", "成果", "奖励",
                "技能", "研究方向", "联系方式");
    }

    private String generateBio(ResumeParsedData data) {
        StringBuilder builder = new StringBuilder();

        if (notBlank(data.getOrganization())) {
            builder.append("现任职于").append(data.getOrganization()).append("。");
        }

        if (notBlank(data.getPositionTitle())) {
            builder.append("职务或职称为").append(data.getPositionTitle()).append("。");
        }

        if (notBlank(data.getResearchDirection())) {
            builder.append("主要研究方向包括").append(data.getResearchDirection()).append("。");
        }

        return limitLength(builder.toString(), MAX_BIO_LENGTH);
    }

    private Map<String, String> buildConfidence(ResumeParsedData data) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("memberName", confidence(data.getMemberName(), "MEDIUM"));
        map.put("email", confidence(data.getEmail(), "HIGH"));
        map.put("phone", confidence(data.getPhone(), "HIGH"));
        map.put("gender", confidence(data.getGender(), "LOW"));
        map.put("birthYear", data.getBirthYear() == null ? "NONE" : "MEDIUM");
        map.put("countryRegion", confidence(data.getCountryRegion(), "LOW"));
        map.put("organization", confidence(data.getOrganization(), "MEDIUM"));
        map.put("department", confidence(data.getDepartment(), "MEDIUM"));
        map.put("positionTitle", confidence(data.getPositionTitle(), "MEDIUM"));
        map.put("highestDegree", confidence(data.getHighestDegree(), "MEDIUM"));
        map.put("professionalField", confidence(data.getProfessionalField(), "LOW"));
        map.put("researchDirection", confidence(data.getResearchDirection(), "LOW"));
        map.put("personalBio", confidence(data.getPersonalBio(), "LOW"));
        map.put("representativeAchievements", confidence(data.getRepresentativeAchievements(), "LOW"));
        map.put("homepage", confidence(data.getHomepage(), "HIGH"));
        map.put("orcid", confidence(data.getOrcid(), "HIGH"));
        map.put("scholarProfile", confidence(data.getScholarProfile(), "NONE"));

        return map;
    }

    private Map<String, String> emptyConfidence() {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("memberName", "NONE");
        map.put("email", "NONE");
        map.put("phone", "NONE");
        map.put("gender", "NONE");
        map.put("birthYear", "NONE");
        map.put("countryRegion", "NONE");
        map.put("organization", "NONE");
        map.put("department", "NONE");
        map.put("positionTitle", "NONE");
        map.put("highestDegree", "NONE");
        map.put("professionalField", "NONE");
        map.put("researchDirection", "NONE");
        map.put("personalBio", "NONE");
        map.put("representativeAchievements", "NONE");
        map.put("homepage", "NONE");
        map.put("orcid", "NONE");
        map.put("scholarProfile", "NONE");

        return map;
    }

    private String confidence(String value, String presentConfidence) {
        return notBlank(value) ? presentConfidence : "NONE";
    }

    private String extractFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group().trim();
        }

        return "";
    }

    private List<String> extractAll(Pattern pattern, String text) {
        List<String> result = new ArrayList<>();

        if (text == null) {
            return result;
        }

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String value = matcher.group();

            if (value != null && !value.trim().isEmpty()) {
                result.add(value.trim());
            }
        }

        return result;
    }

    private String afterColon(String line) {
        if (line == null) {
            return "";
        }

        int index = line.indexOf(":");

        if (index < 0 || index >= line.length() - 1) {
            return "";
        }

        return line.substring(index + 1).trim();
    }

    private String best(List<ScoredText> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }

        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        return candidates.get(0).text;
    }

    private int count(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return 0;
        }

        int result = 0;
        int index = 0;

        while ((index = text.indexOf(keyword, index)) >= 0) {
            result++;
            index += keyword.length();
        }

        return result;
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty() && text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsAny(String text, Collection<String> keywords) {
        if (text == null || keywords == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty() && text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        return cleaned.substring(0, maxLength);
    }

    private static class ScoredText {
        private final String text;
        private final int score;

        private ScoredText(String text, int score) {
            this.text = text;
            this.score = score;
        }
    }

    public static class ResumeExtractResult {

        private String parseStatus;
        private String message;
        private String extractedText;
        private ResumeParsedData parsedData;
        private Map<String, String> confidence;

        public String getParseStatus() {
            return parseStatus;
        }

        public void setParseStatus(String parseStatus) {
            this.parseStatus = parseStatus;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getExtractedText() {
            return extractedText;
        }

        public void setExtractedText(String extractedText) {
            this.extractedText = extractedText;
        }

        public ResumeParsedData getParsedData() {
            return parsedData;
        }

        public void setParsedData(ResumeParsedData parsedData) {
            this.parsedData = parsedData;
        }

        public Map<String, String> getConfidence() {
            return confidence;
        }

        public void setConfidence(Map<String, String> confidence) {
            this.confidence = confidence;
        }
    }

}