package com.ruoyi.web.controller.ai.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * PDF 排版还原提取器
 * 按 y 坐标对文本进行排序，检测两栏布局并还原阅读顺序
 * 解决两栏简历左右栏文字交错问题
 */
public class CoordinateTextStripper extends PDFTextStripper {

    private static final Logger log = LoggerFactory.getLogger(CoordinateTextStripper.class);

    private final List<TextSegment> segments = new ArrayList<>();
    private StringBuilder currentSegmentText = null;
    private float currentSegX = 0;
    private float currentSegY = 0;
    private float currentSegWidth = 0;

    public CoordinateTextStripper() throws IOException {
        super();
        setSortByPosition(true);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) {
        for (TextPosition pos : textPositions) {
            String ch = pos.getUnicode();
            if (ch == null || ch.isEmpty()) continue;
            segments.add(new TextSegment(pos.getX(), pos.getY(), pos.getWidth(), ch));
        }
    }

    /**
     * 提取文本并按布局还原阅读顺序
     */
    public String getReconstructedText(PDDocument doc) throws IOException {
        segments.clear();

        // 触发 PDFBox 解析流程，writeString 会被调用收集 segment
        super.getText(doc);

        if (segments.isEmpty()) return "";

        // 1. 按 y 排序（从上到下），y 相同时按 x（从左到右）
        segments.sort((a, b) -> {
            int yCmp = Float.compare(a.y, b.y);
            if (yCmp != 0) return yCmp;
            return Float.compare(a.x, b.x);
        });

        // 2. 合并成文本行（y 坐标相近视为同一行）
        float lineHeight = estimateLineHeight();
        List<TextLine> lines = new ArrayList<>();
        TextLine currentLine = null;
        for (TextSegment seg : segments) {
            if (currentLine == null || Math.abs(seg.y - currentLine.y) > lineHeight * 0.7f) {
                currentLine = new TextLine(seg.y);
                lines.add(currentLine);
            }
            currentLine.addSegment(seg);
        }

        // 3. 检测是否两栏布局
        boolean twoColumn = detectTwoColumn(lines, lineHeight);

        // 4. 还原文本
        StringBuilder result = new StringBuilder();
        if (twoColumn) {
            float splitX = findColumnSplitX(lines);
            List<TextLine> leftCol = new ArrayList<>();
            List<TextLine> rightCol = new ArrayList<>();
            for (TextLine line : lines) {
                TextLine leftPart = new TextLine(line.y);
                TextLine rightPart = new TextLine(line.y);
                for (TextSegment seg : line.segments) {
                    if (seg.x + seg.width / 2 < splitX) {
                        leftPart.addSegment(seg);
                    } else {
                        rightPart.addSegment(seg);
                    }
                }
                if (!leftPart.segments.isEmpty()) leftCol.add(leftPart);
                if (!rightPart.segments.isEmpty()) rightCol.add(rightPart);
            }
            // 先输出左栏所有内容，再输出右栏所有内容
            for (TextLine line : leftCol) {
                result.append(line.getText()).append("\n");
            }
            for (TextLine line : rightCol) {
                result.append(line.getText()).append("\n");
            }
        } else {
            // 单栏：直接按顺序输出
            for (TextLine line : lines) {
                result.append(line.getText()).append("\n");
            }
        }

        String out = result.toString().trim();
        log.info("PDF 排版还原完成: {} chars, {} lines, twoColumn={}", out.length(), lines.size(), twoColumn);
        return out;
    }

    private float estimateLineHeight() {
        if (segments.size() < 10) return 14f;
        // 统计 y 间距分布，取最常见的间距作为行高
        Map<Integer, Integer> gapCount = new HashMap<>();
        float prevY = segments.get(0).y;
        for (int i = 1; i < segments.size(); i++) {
            float diff = segments.get(i).y - prevY;
            int gap = Math.round(diff);
            if (gap > 2 && gap < 80) {
                gapCount.merge(gap, 1, Integer::sum);
            }
            prevY = segments.get(i).y;
        }
        return gapCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> (float) e.getKey())
                .orElse(14f);
    }

    private boolean detectTwoColumn(List<TextLine> lines, float lineHeight) {
        if (lines.size() < 8) return false;
        // 统计：有多少行内部存在大间距（超过页面宽度的 25%）
        int gapCount = 0;
        for (TextLine line : lines) {
            List<TextSegment> segs = line.segments;
            if (segs.size() < 3) continue;
            float minX = Float.MAX_VALUE, maxX = 0;
            for (TextSegment s : segs) {
                if (s.x < minX) minX = s.x;
                float right = s.x + s.width;
                if (right > maxX) maxX = right;
            }
            float span = maxX - minX;
            if (span < 30) continue;
            for (int i = 1; i < segs.size(); i++) {
                float gap = segs.get(i).x - (segs.get(i - 1).x + segs.get(i - 1).width);
                if (gap > span * 0.3f) {
                    gapCount++;
                    break;
                }
            }
        }
        // 超过 20% 的行存在大间距 → 两栏
        return gapCount > lines.size() * 0.2f;
    }

    private float findColumnSplitX(List<TextLine> lines) {
        // 收集所有 segment 的 x 坐标（去重）
        List<Float> allX = new ArrayList<>();
        for (TextLine line : lines) {
            for (TextSegment seg : line.segments) {
                allX.add(seg.x);
            }
        }
        if (allX.size() < 5) return 0f;
        Collections.sort(allX);
        // 找中间段的最大间隙
        int startIdx = (int) (allX.size() * 0.35);
        int endIdx = (int) (allX.size() * 0.65);
        float maxGap = 0;
        float splitX = allX.get(allX.size() / 2);
        for (int i = startIdx; i < Math.min(endIdx, allX.size() - 1); i++) {
            float gap = allX.get(i + 1) - allX.get(i);
            if (gap > maxGap) {
                maxGap = gap;
                splitX = (allX.get(i) + allX.get(i + 1)) / 2;
            }
        }
        return splitX;
    }

    static class TextSegment {
        float x, y, width;
        String text;

        TextSegment(float x, float y, float width, String text) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.text = text;
        }
    }

    static class TextLine {
        float y;
        List<TextSegment> segments = new ArrayList<>();

        TextLine(float y) {
            this.y = y;
        }

        void addSegment(TextSegment seg) {
            segments.add(seg);
        }

        String getText() {
            segments.sort(Comparator.comparing(a -> a.x));
            StringBuilder sb = new StringBuilder();
            float lastRight = -1;
            for (TextSegment s : segments) {
                if (lastRight >= 0 && s.x - lastRight > 3f) {
                    sb.append(" ");
                }
                sb.append(s.text);
                lastRight = s.x + s.width;
            }
            return sb.toString();
        }
    }
}
