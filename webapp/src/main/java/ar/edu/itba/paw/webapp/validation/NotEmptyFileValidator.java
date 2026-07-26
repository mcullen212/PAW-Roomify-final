package ar.edu.itba.paw.webapp.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class NotEmptyFileValidator implements ConstraintValidator<NotEmptyFile, MultipartFile[]> {
    private final List<String> allowedExtensions = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    @Override
    public boolean isValid(MultipartFile[] files, ConstraintValidatorContext context) {
        if (files == null || files.length == 0) return false;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) return false;

            String filename = file.getOriginalFilename().toLowerCase();
            boolean valid = allowedExtensions.stream().anyMatch(filename::endsWith);
            if (!valid) return false;
            if (file.getSize() > 5 * 1024 * 1024) return false; // max 5MB
        }
        return true;
    }
}

