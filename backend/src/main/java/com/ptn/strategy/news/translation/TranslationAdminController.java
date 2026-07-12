package com.ptn.strategy.news.translation;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
public class TranslationAdminController {

    private final TranslationAdminService adminService;

    public TranslationAdminController(TranslationAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/{articleId}/retranslate")
    public ResponseEntity<Map<String, Object>> retranslate(@PathVariable long articleId) {
        boolean accepted = adminService.retranslate(articleId);
        if (!accepted) {
            return ResponseEntity.badRequest().body(Map.of(
                    "articleId", articleId,
                    "accepted", false,
                    "message", "Article is not in TRANSLATION_FAILED/TRANSLATION_DEAD state"));
        }
        return ResponseEntity.accepted().body(Map.of("articleId", articleId, "accepted", true));
    }

    @PostMapping("/{articleId}/reindex")
    public ResponseEntity<Map<String, Object>> reindex(@PathVariable long articleId) {
        boolean accepted = adminService.reindex(articleId);
        if (!accepted) {
            return ResponseEntity.badRequest().body(Map.of(
                    "articleId", articleId,
                    "accepted", false,
                    "message", "Article is not in an index retryable state"));
        }
        return ResponseEntity.accepted().body(Map.of("articleId", articleId, "accepted", true));
    }
}
