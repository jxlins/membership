package com.jxl.membership.toolkit;

import com.jxl.membership.common.constant.MemberConstants;
import com.jxl.membership.common.exception.BusinessException;
import com.jxl.membership.config.SocietyFileProperties;
import com.jxl.membership.dto.FileSaveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileStorageToolkit {

    private final SocietyFileProperties societyFileProperties;

    public FileSaveResult saveResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Resume file is required.");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename());
        if (!originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BusinessException("Only PDF resume files are supported.");
        }
        if (file.getSize() > MemberConstants.MAX_RESUME_SIZE) {
            throw new BusinessException("Resume file is too large. The maximum size is 10MB.");
        }

        try {
            String datePath = LocalDate.now().toString().replace("-", "/");
            Path directory = Paths.get(societyFileProperties.getUploadRoot(), "resumes", datePath).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID() + ".pdf";
            Path target = directory.resolve(storedName).normalize();
            file.transferTo(target);

            String publicPrefix = societyFileProperties.getPublicPrefix().replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
            String url = "/" + publicPrefix + "/resumes/" + datePath + "/" + storedName;

            FileSaveResult result = new FileSaveResult();
            result.setFileUrl(url);
            result.setOriginalName(originalFilename);
            result.setFileType(file.getContentType() == null ? "application/pdf" : file.getContentType());
            result.setFileSize(file.getSize());
            result.setAbsolutePath(target.toString());
            return result;
        } catch (IOException exception) {
            throw new BusinessException("Failed to save resume file.", exception);
        }
    }

    public void deleteQuietly(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return;
        }
        try {
            String prefix = "/" + societyFileProperties.getPublicPrefix().replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "") + "/";
            if (!fileUrl.startsWith(prefix)) {
                return;
            }
            String relativePath = fileUrl.substring(prefix.length()).replace("/", java.io.File.separator);
            Path target = Paths.get(societyFileProperties.getUploadRoot(), relativePath).toAbsolutePath().normalize();
            Path root = Paths.get(societyFileProperties.getUploadRoot()).toAbsolutePath().normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (Exception ignored) {
            // Old file cleanup must not affect the committed member data.
        }
    }
}
