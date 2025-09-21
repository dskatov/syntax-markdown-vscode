package org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class MathContentPlaceholderProcessor
{
    private static final char SENTINEL = '\u0007';
    private static final String INLINE_PREFIX = "MI";
    private static final String BLOCK_PREFIX = "MB";

    private static final ThreadLocal<Map<String, MathToken>> TOKENS = new ThreadLocal<>();

    private MathContentPlaceholderProcessor()
    {
    }

    static Result preprocess(String source)
    {
        if (source == null || source.isEmpty()) {
            return new Result(source, Collections.emptyMap());
        }

        StringBuilder output = new StringBuilder(source.length());
        Map<String, MathToken> tokens = new LinkedHashMap<>();
        int index = 0;
        int inlineCounter = 0;
        int blockCounter = 0;

        while (index < source.length()) {
            char current = source.charAt(index);
            if (current != '$') {
                output.append(current);
                index++;
                continue;
            }

            int run = countRun(source, index);
            if (run >= 2 && !isEscaped(source, index)) {
                int closing = findBlockClosing(source, index + 2);
                if (closing == -1) {
                    output.append(source.substring(index));
                    break;
                }

                String between = source.substring(index + 2, closing);
                String stripped = stripEnclosingLineBreaks(between);
                if (stripped.trim().isEmpty()) {
                    output.append(source, index, closing + 2);
                    index = closing + 2;
                    continue;
                }

                String placeholder = createPlaceholder(false, blockCounter++);
                tokens.put(placeholder, new MathToken(between, false));
                output.append(placeholder);
                index = closing + 2;
                continue;
            }

            if (run == 1 && !isEscaped(source, index)) {
                int closing = findInlineClosing(source, index + 1);
                if (closing == -1) {
                    output.append(source.substring(index));
                    break;
                }

                String between = source.substring(index + 1, closing);
                String trimmed = between.trim();
                if (trimmed.isEmpty() || containsLineBreak(trimmed)) {
                    output.append(source, index, closing + 1);
                    index = closing + 1;
                    continue;
                }

                String placeholder = createPlaceholder(true, inlineCounter++);
                tokens.put(placeholder, new MathToken(trimmed, true));
                output.append(placeholder);
                index = closing + 1;
                continue;
            }

            output.append(current);
            index++;
        }

        return new Result(output.toString(), tokens);
    }

    static void pushTokens(Map<String, MathToken> tokens)
    {
        if (tokens == null || tokens.isEmpty()) {
            TOKENS.remove();
        } else {
            TOKENS.set(new LinkedHashMap<>(tokens));
        }
    }

    static void clear()
    {
        TOKENS.remove();
    }

    static boolean hasTokens()
    {
        Map<String, MathToken> tokens = TOKENS.get();
        return tokens != null && !tokens.isEmpty();
    }

    static PlaceholderMatch findNextPlaceholder(String text, int fromIndex)
    {
        Map<String, MathToken> tokens = TOKENS.get();
        if (tokens == null || tokens.isEmpty() || text == null) {
            return null;
        }

        int start = text.indexOf(SENTINEL, fromIndex);
        while (start != -1) {
            int end = text.indexOf(SENTINEL, start + 1);
            if (end == -1) {
                return null;
            }
            String placeholder = text.substring(start, end + 1);
            if (tokens.containsKey(placeholder)) {
                return new PlaceholderMatch(start, end + 1, placeholder);
            }
            start = text.indexOf(SENTINEL, end + 1);
        }
        return null;
    }

    static MathToken peek(String placeholder)
    {
        Map<String, MathToken> tokens = TOKENS.get();
        if (tokens == null) {
            return null;
        }
        return tokens.get(placeholder);
    }

    static MathToken consume(String placeholder)
    {
        Map<String, MathToken> tokens = TOKENS.get();
        if (tokens == null) {
            return null;
        }
        return tokens.remove(placeholder);
    }

    static char getSentinel()
    {
        return SENTINEL;
    }

    private static String createPlaceholder(boolean inline, int index)
    {
        String prefix = inline ? INLINE_PREFIX : BLOCK_PREFIX;
        return new StringBuilder()
            .append(SENTINEL)
            .append(prefix)
            .append(index)
            .append(SENTINEL)
            .toString();
    }

    private static int countRun(String text, int start)
    {
        int index = start;
        while (index < text.length() && text.charAt(index) == '$') {
            index++;
        }
        return index - start;
    }

    private static int findBlockClosing(String text, int searchStart)
    {
        int index = searchStart;
        while (index < text.length() - 1) {
            int candidate = text.indexOf("$$", index);
            if (candidate == -1) {
                return -1;
            }
            if (!isEscaped(text, candidate)) {
                return candidate;
            }
            index = candidate + 2;
        }
        return -1;
    }

    private static int findInlineClosing(String text, int searchStart)
    {
        int index = searchStart;
        while (index < text.length()) {
            int candidate = text.indexOf('$', index);
            if (candidate == -1) {
                return -1;
            }
            if (!isEscaped(text, candidate) && countRun(text, candidate) == 1) {
                return candidate;
            }
            index = candidate + 1;
        }
        return -1;
    }

    private static boolean isEscaped(String text, int position)
    {
        int backslashes = 0;
        for (int i = position - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return (backslashes & 1) == 1;
    }

    private static boolean containsLineBreak(String text)
    {
        return text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
    }

    private static String stripEnclosingLineBreaks(String value)
    {
        int start = 0;
        int end = value.length();
        while (start < end && isLineBreak(value.charAt(start))) {
            start++;
        }
        while (end > start && isLineBreak(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isLineBreak(char character)
    {
        return character == '\n' || character == '\r';
    }

    static final class Result
    {
        private final String content;
        private final Map<String, MathToken> tokens;

        Result(String content, Map<String, MathToken> tokens)
        {
            this.content = content;
            this.tokens = tokens;
        }

        String getContent()
        {
            return this.content;
        }

        Map<String, MathToken> getTokens()
        {
            return this.tokens;
        }
    }

    static final class MathToken
    {
        private final String content;
        private final boolean inline;

        MathToken(String content, boolean inline)
        {
            this.content = content;
            this.inline = inline;
        }

        String getContent()
        {
            return this.content;
        }

        boolean isInline()
        {
            return this.inline;
        }
    }

    static final class PlaceholderMatch
    {
        private final int start;
        private final int end;
        private final String placeholder;

        PlaceholderMatch(int start, int end, String placeholder)
        {
            this.start = start;
            this.end = end;
            this.placeholder = placeholder;
        }

        int getStart()
        {
            return this.start;
        }

        int getEnd()
        {
            return this.end;
        }

        String getPlaceholder()
        {
            return this.placeholder;
        }
    }
}
