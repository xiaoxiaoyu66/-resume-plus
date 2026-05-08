package com.ruoyi.web.controller.ai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文件向量化服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class FileVectorizationServiceTest {

    @Mock
    private FileContentService fileContentService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorService vectorService;

    @InjectMocks
    private FileVectorizationService fileVectorizationService;

    @Test
    void vectorizeFile_shouldReturnSavedCount_whenContentIsValid() {
        String fullText = "Java是一种编程语言。Spring Boot是Java框架。";
        FileContentService.FileContent content = new FileContentService.FileContent("test.txt", fullText);
        List<String> chunks = List.of("Java是一种编程语言", "Spring Boot是Java框架");

        when(fileContentService.parseFiles(List.of("test.txt"))).thenReturn(List.of(content));
        when(embeddingService.chunkTextSmart(fullText, 800)).thenReturn(chunks);
        when(vectorService.saveDocumentVectors(1L, 100L, chunks)).thenReturn(chunks.size());

        int result = fileVectorizationService.vectorizeFile(1L, "test.txt", 100L);

        assertEquals(2, result);
        verify(vectorService).saveDocumentVectors(1L, 100L, chunks);
    }

    @Test
    void vectorizeFile_shouldReturnZero_whenContentIsEmpty() {
        // Note: current code doesn't guard against empty content;
        // it passes through to chunkTextSmart which returns empty list,
        // then saveDocumentVectors with empty list returns 0.
        FileContentService.FileContent content = new FileContentService.FileContent("test.txt", "");
        when(fileContentService.parseFiles(List.of("test.txt"))).thenReturn(List.of(content));
        when(embeddingService.chunkTextSmart("", 800)).thenReturn(List.of());
        when(vectorService.saveDocumentVectors(1L, 100L, List.of())).thenReturn(0);

        int result = fileVectorizationService.vectorizeFile(1L, "test.txt", 100L);

        assertEquals(0, result);
    }

    @Test
    void vectorizeFile_shouldReturnZero_whenContentStartsWithBracket() {
        // Content starting with "[" is considered an error/JSON response
        FileContentService.FileContent content = new FileContentService.FileContent("test.txt", "[error: cannot parse]");
        when(fileContentService.parseFiles(List.of("test.txt"))).thenReturn(List.of(content));

        int result = fileVectorizationService.vectorizeFile(1L, "test.txt", 100L);

        assertEquals(0, result);
        verifyNoInteractions(embeddingService, vectorService);
    }

    @Test
    void vectorizeFile_shouldReturnZero_whenParseReturnsEmptyList() {
        when(fileContentService.parseFiles(List.of("test.txt"))).thenReturn(List.of());

        int result = fileVectorizationService.vectorizeFile(1L, "test.txt", 100L);

        assertEquals(0, result);
        verifyNoInteractions(embeddingService, vectorService);
    }

    @Test
    void vectorizeFile_shouldReturnZero_whenExceptionOccurs() {
        when(fileContentService.parseFiles(List.of("test.txt")))
                .thenThrow(new RuntimeException("MinIO connection failed"));

        int result = fileVectorizationService.vectorizeFile(1L, "test.txt", 100L);

        assertEquals(0, result);
        verifyNoInteractions(embeddingService, vectorService);
    }

    @Test
    void vectorizeFile_shouldHandleChunkingWithNullContent() {
        FileContentService.FileContent content = new FileContentService.FileContent("test.txt", null);
        when(fileContentService.parseFiles(List.of("test.txt"))).thenReturn(List.of(content));
        // Null content will cause NullPointerException in startsWith check,
        // which is caught and returns 0
        int result = fileVectorizationService.vectorizeFile(1L, "test.txt", 100L);

        assertEquals(0, result);
    }

    @Test
    void deleteFileVectors_shouldDelegateToVectorService() {
        fileVectorizationService.deleteFileVectors(42L);

        verify(vectorService).deleteByFileId(42L);
    }

    @Test
    void getVectorCount_shouldReturnCountFromVectorService() {
        when(vectorService.getVectorCount(7L)).thenReturn(5);

        int count = fileVectorizationService.getVectorCount(7L);

        assertEquals(5, count);
        verify(vectorService).getVectorCount(7L);
    }

    @Test
    void vectorizeFile_shouldPassCorrectChunkSize() {
        String longText = "A".repeat(2000);
        FileContentService.FileContent content = new FileContentService.FileContent("long.txt", longText);
        List<String> chunks = List.of("chunk1", "chunk2", "chunk3");

        when(fileContentService.parseFiles(List.of("long.txt"))).thenReturn(List.of(content));
        when(embeddingService.chunkTextSmart(longText, 800)).thenReturn(chunks);
        when(vectorService.saveDocumentVectors(2L, 200L, chunks)).thenReturn(3);

        int result = fileVectorizationService.vectorizeFile(2L, "long.txt", 200L);

        assertEquals(3, result);
        verify(embeddingService).chunkTextSmart(longText, 800);
    }
}
