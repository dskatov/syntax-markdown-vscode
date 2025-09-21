/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.contrib.rendering.markdown.commonmark12.internal.MarkdownConfiguration;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.StreamParser;

import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;

/**
 * Handle paragraph events and upgrade $$ math blocks to macros.
 *
 * @version $Id$
 * @since 8.4
 */
public class ParagraphNodeVisitor extends AbstractNodeVisitor
{
    private final MarkdownConfiguration configuration;

    private final Set<Node> nodesToSkip = new HashSet<>();

    static <V extends ParagraphNodeVisitor> VisitHandler<?>[] VISIT_HANDLERS(final V visitor)
    {
        return new VisitHandler<?>[]{
                new VisitHandler<>(Paragraph.class, node -> visitor.visit(node))
        };
    }

    public ParagraphNodeVisitor(NodeVisitor visitor, Deque<Listener> listeners,
        MarkdownConfiguration configuration, org.xwiki.rendering.parser.StreamParser plainTextStreamParser)
    {
        super(visitor, listeners, null, plainTextStreamParser);
        this.configuration = configuration;
    }

    public void visit(Paragraph node)
    {
        if (this.nodesToSkip.remove(node)) {
            return;
        }

        if (emitBlockMath(node)) {
            return;
        }

        getListener().beginParagraph(Collections.emptyMap());
        getVisitor().visitChildren(node);
        getListener().endParagraph(Collections.emptyMap());
    }

    private boolean emitBlockMath(Paragraph node)
    {
        String raw = node.getChars().toString();
        String trimmed = raw.trim();

        if (trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length() > 4
            && trimmed.indexOf("$$", 2) == trimmed.length() - 2)
        {
            String content = trimmed.substring(2, trimmed.length() - 2);
            content = stripEnclosingLineBreaks(content);
            if (content.trim().isEmpty()) {
                return false;
            }

            emitBlockMacro(content);
            return true;
        }

        if ("$$".equals(trimmed)) {
            List<Paragraph> between = new ArrayList<>();
            Paragraph closing = null;
            Node current = node.getNext();
            while (current != null) {
                if (current instanceof Paragraph) {
                    String currentTrimmed = current.getChars().toString().trim();
                    if ("$$".equals(currentTrimmed)) {
                        closing = (Paragraph) current;
                        break;
                    }
                    between.add((Paragraph) current);
                    current = current.getNext();
                    continue;
                }
                break;
            }

            if (closing == null) {
                return false;
            }

            StringBuilder builder = new StringBuilder();
            for (Paragraph paragraph : between) {
                builder.append(paragraph.getChars().toString());
            }
            String content = stripEnclosingLineBreaks(builder.toString());
            if (content.trim().isEmpty()) {
                return false;
            }

            emitBlockMacro(content);
            this.nodesToSkip.addAll(between);
            this.nodesToSkip.add(closing);
            return true;
        }

        return emitEmbeddedBlockMath(node);
    }

    private String stripEnclosingLineBreaks(String value)
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

    private boolean isLineBreak(char character)
    {
        return character == '\n' || character == '\r';
    }

    private void emitBlockMacro(String content)
    {
        String wrapped = ensureBlockWrapped(content);
        getListener().onMacro(this.configuration.getMathMacroId(),
            this.configuration.getBlockMathMacroParameters(), wrapped, false);
    }

    private String ensureBlockWrapped(String content)
    {
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        if ((trimmed.startsWith("\\[") && trimmed.endsWith("\\]"))
            || (trimmed.startsWith("\\(") && trimmed.endsWith("\\)")))
        {
            return trimmed;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\\[\n").append(trimmed).append('\n').append("\\]");
        return builder.toString();
    }

    /**
     * Handle paragraphs that mix regular text with display math delimited by {@code $$}.
     */
    private boolean emitEmbeddedBlockMath(Paragraph node)
    {
        List<Object> segments = new ArrayList<>();
        StringBuilder plainAccumulator = new StringBuilder();
        boolean foundBlock = false;

        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Text) {
                String value = child.getChars().toString();
                int index = 0;
                while (index < value.length()) {
                    int open = value.indexOf("$$", index);
                    if (open == -1) {
                        plainAccumulator.append(value.substring(index));
                        break;
                    }

                    int close = findClosingDoubleDollar(value, open + 2);
                    if (close == -1) {
                        plainAccumulator.append(value.substring(index));
                        break;
                    }

                    plainAccumulator.append(value, index, open);
                    String content = value.substring(open + 2, close);
                    String stripped = stripEnclosingLineBreaks(content);
                    if (stripped.trim().isEmpty()) {
                        plainAccumulator.append(value, open, close + 2);
                    } else {
                        addPlainSegment(segments, plainAccumulator);
                        segments.add(new BlockMathSegment(stripped));
                        foundBlock = true;
                    }

                    index = close + 2;
                }
            } else if (child instanceof SoftLineBreak) {
                plainAccumulator.append(' ');
            } else if (child instanceof HardLineBreak) {
                plainAccumulator.append("\n");
            } else {
                addPlainSegment(segments, plainAccumulator);
                segments.add(child);
            }
        }

        addPlainSegment(segments, plainAccumulator);

        if (!foundBlock) {
            return false;
        }

        boolean paragraphOpen = false;
        for (Object segment : segments) {
            if (segment instanceof String) {
                String textSegment = (String) segment;
                if (!textSegment.isEmpty()) {
                    paragraphOpen = ensureParagraph(paragraphOpen);
                    visitTextSegment(textSegment);
                }
            } else if (segment instanceof BlockMathSegment) {
                paragraphOpen = closeParagraph(paragraphOpen);
                emitBlockMacro(((BlockMathSegment) segment).getContent());
            } else if (segment instanceof Node) {
                paragraphOpen = ensureParagraph(paragraphOpen);
                getVisitor().visit((Node) segment);
            }
        }

        closeParagraph(paragraphOpen);
        return true;
    }

    private void visitTextSegment(String textSegment)
    {
        if (!textSegment.isEmpty()) {
            getVisitor().visit(new Text(textSegment));
        }
    }

    private void addPlainSegment(List<Object> segments, StringBuilder accumulator)
    {
        if (accumulator.length() > 0) {
            segments.add(accumulator.toString());
            accumulator.setLength(0);
        }
    }

    private static class BlockMathSegment
    {
        private final String content;

        BlockMathSegment(String content)
        {
            this.content = content;
        }

        String getContent()
        {
            return this.content;
        }
    }

    private int findClosingDoubleDollar(String text, int searchStart)
    {
        return text.indexOf("$$", searchStart);
    }

    private boolean ensureParagraph(boolean paragraphOpen)
    {
        if (!paragraphOpen) {
            getListener().beginParagraph(Collections.emptyMap());
            paragraphOpen = true;
        }
        return paragraphOpen;
    }

    private boolean closeParagraph(boolean paragraphOpen)
    {
        if (paragraphOpen) {
            getListener().endParagraph(Collections.emptyMap());
        }
        return false;
    }
}
