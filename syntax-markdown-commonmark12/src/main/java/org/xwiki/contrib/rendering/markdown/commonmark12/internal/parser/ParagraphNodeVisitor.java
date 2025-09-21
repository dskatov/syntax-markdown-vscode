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

import java.util.Collections;
import java.util.Deque;

import org.xwiki.contrib.rendering.markdown.commonmark12.internal.MarkdownConfiguration;
import org.xwiki.rendering.listener.Listener;

import com.vladsch.flexmark.ast.Paragraph;
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

    static <V extends ParagraphNodeVisitor> VisitHandler<?>[] VISIT_HANDLERS(final V visitor)
    {
        return new VisitHandler<?>[]{
                new VisitHandler<>(Paragraph.class, node -> visitor.visit(node))
        };
    }

    public ParagraphNodeVisitor(NodeVisitor visitor, Deque<Listener> listeners,
        MarkdownConfiguration configuration)
    {
        super(visitor, listeners);
        this.configuration = configuration;
    }

    public void visit(Paragraph node)
    {
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
        if (!trimmed.startsWith("$$") || !trimmed.endsWith("$$") || trimmed.length() <= 4) {
            return false;
        }

        if (trimmed.indexOf("$$", 2) != trimmed.length() - 2) {
            return false;
        }

        String content = trimmed.substring(2, trimmed.length() - 2);
        content = stripEnclosingLineBreaks(content);
        if (content.trim().isEmpty()) {
            return false;
        }

        getListener().onMacro(this.configuration.getMathMacroId(),
            this.configuration.getBlockMathMacroParameters(), content.trim(), false);
        return true;
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
}
