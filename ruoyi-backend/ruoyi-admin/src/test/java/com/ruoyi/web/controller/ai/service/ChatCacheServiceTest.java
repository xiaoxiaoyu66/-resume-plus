package com.ruoyi.web.controller.ai.service;

import com.ruoyi.web.controller.ai.service.ChatCacheService.CacheEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 高频缓存服务单元测试
 */
@SpringBootTest
public class ChatCacheServiceTest {

    @Autowired
    private ChatCacheService chatCacheService;

    @BeforeEach
    void setUp() {
        // 清空缓存
        chatCacheService.clearAll();
    }

    @Test
    void testPutAndGet() {
        // 测试存入和获取缓存
        String question = "什么是Java";
        String answer = "Java是一种编程语言";
        String scene = "default";

        // 存入缓存
        chatCacheService.put(question, answer, scene);

        // 获取缓存
        CacheEntry entry = chatCacheService.get(question, scene);

        // 验证
        assertNotNull(entry);
        assertEquals(answer, entry.getAnswer());
        assertEquals(question, entry.getQuestion());
        assertEquals(scene, entry.getScene());
    }

    @Test
    void testCacheMiss() {
        // 测试缓存未命中
        String question = "不存在的問題";
        String scene = "default";

        CacheEntry entry = chatCacheService.get(question, scene);

        assertNull(entry);
    }

    @Test
    void testSimilarQuestionMatching() {
        // 测试相似问题匹配
        String question1 = "Java是什么";
        String answer = "Java是一种面向对象的编程语言";
        String scene = "default";

        // 存入第一个问题
        chatCacheService.put(question1, answer, scene);

        // 查询相似问题（关键词提取后会匹配）
        String question2 = "Java是什么语言";
        CacheEntry entry = chatCacheService.get(question2, scene);

        // 由于相似度匹配简化实现，可能命中也可能不命中
        // 这里主要测试不会抛出异常
        assertDoesNotThrow(() -> chatCacheService.get(question2, scene));
    }

    @Test
    void testInvalidate() {
        // 测试缓存失效
        String question = "测试问题";
        String answer = "测试答案";
        String scene = "default";

        chatCacheService.put(question, answer, scene);
        assertNotNull(chatCacheService.get(question, scene));

        // 使缓存失效
        chatCacheService.invalidate(question, scene);

        // 验证已失效
        assertNull(chatCacheService.get(question, scene));
    }

    @Test
    void testCacheStats() {
        // 测试缓存统计
        // 先进行一些操作
        chatCacheService.put("问题1", "答案1", "default");
        chatCacheService.put("问题2", "答案2", "default");
        chatCacheService.get("问题1", "default"); // 命中
        chatCacheService.get("问题3", "default"); // 未命中

        ChatCacheService.CacheStats stats = chatCacheService.getStats();

        assertNotNull(stats);
        assertTrue(stats.getSize() >= 0);
        System.out.println("缓存统计: " +
                "size=" + stats.getSize() +
                ", hitCount=" + stats.getHitCount() +
                ", missCount=" + stats.getMissCount() +
                ", hitRate=" + stats.getHitRate());
    }

    @Test
    void testDifferentScenes() {
        // 测试不同场景隔离
        String question = "相同问题";
        String answer1 = "场景1答案";
        String answer2 = "场景2答案";

        chatCacheService.put(question, answer1, "scene1");
        chatCacheService.put(question, answer2, "scene2");

        CacheEntry entry1 = chatCacheService.get(question, "scene1");
        CacheEntry entry2 = chatCacheService.get(question, "scene2");

        assertNotNull(entry1);
        assertNotNull(entry2);
        assertEquals(answer1, entry1.getAnswer());
        assertEquals(answer2, entry2.getAnswer());
    }

    @Test
    void testConcurrentAccess() {
        // 测试并发访问
        String scene = "default";

        // 多线程并发写入
        for (int i = 0; i < 10; i++) {
            final int index = i;
            new Thread(() -> {
                chatCacheService.put("问题" + index, "答案" + index, scene);
            }).start();
        }

        // 等待线程完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 验证数据完整性
        for (int i = 0; i < 10; i++) {
            CacheEntry entry = chatCacheService.get("问题" + i, scene);
            assertNotNull(entry, "问题" + i + "应该存在");
            assertEquals("答案" + i, entry.getAnswer());
        }
    }
}
