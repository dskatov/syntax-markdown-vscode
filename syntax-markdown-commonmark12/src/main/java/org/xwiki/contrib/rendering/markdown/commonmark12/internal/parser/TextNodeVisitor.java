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

import java.util.Deque;

import org.xwiki.contrib.rendering.markdown.commonmark12.internal.MarkdownConfiguration;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.StreamParser;

import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;

/**
 * Handle text events, including converting inline math spans to macros.
 *
 * @version $Id$
 * @since 8.4
 */
public class TextNodeVisitor extends AbstractNodeVisitor
{
    private final MarkdownConfiguration configuration;

    static <V extends TextNodeVisitor> VisitHandler<?>[] VISIT_HANDLERS(final V visitor)
    {
        return new VisitHandler<?>[]{
                new VisitHandler<>(Text.class, node -> visitor.visit(node))
        };
    }

    public TextNodeVisitor(NodeVisitor visitor, Deque<Listener> listeners,
        StreamParser plainTextStreamParser, MarkdownConfiguration configuration)
    {
        super(visitor, listeners, null, plainTextStreamParser);
        this.configuration = configuration;
    }

    public void visit(Text node)
    {
        process(node.getChars().toString());
        getVisitor().visitChildren(node);
    }

    private void process(String value)
    {
        int index = 0;
        while (index < value.length()) {
            int open = value.indexOf('$', index);
            if (open == -1) {
                emitPlain(value.substring(index));
                break;
            }

            if (isEscaped(value, open)) {
                emitPlain(value.substring(index, open + 1));
                index = open + 1;
                continue;
            }

            int run = countRun(value, open);
            if (run != 1) {
                emitPlain(value.substring(index, open + run));
                index = open + run;
                continue;
            }

            int close = findClosing(value, open + 1);
            if (close == -1) {
                emitPlain(value.substring(index));
                break;
            }

            String between = value.substring(open + 1, close);
            if (between.isEmpty() || containsLineBreak(between)) {
                emitPlain(value.substring(index, close + 1));
                index = close + 1;
                continue;
            }

            emitPlain(value.substring(index, open));
            emitInlineMath(between.trim());
            index = close + 1;
        }
    }

    private void emitPlain(String text)
    {
        if (!text.isEmpty()) {
            parseInline(text);
        }
    }

    private void emitInlineMath(String content)
    {
        if (content.isEmpty()) {
            return;
        }
        getListener().onMacro(this.configuration.getMathMacroId(),
            this.configuration.getInlineMathMacroParameters(), content, true);
    }

    private int findClosing(String text, int start)
    {
        int length = text.length();
        for (int i = start; i < length; i++) {
            if (text.charAt(i) != '$') {
                continue;
            }
            if (isEscaped(text, i)) {
                continue;
            }
            if (countRun(text, i) == 1) {
                return i;
            }
        }
        return -1;
    }

    private int countRun(String text, int start)
    {
        int index = start;
        while (index < text.length() && text.charAt(index) == '$') {
            index++;
        }
        return index - start;
    }

    private boolean isEscaped(String text, int position)
    {
        int backslashes = 0;
        for (int i = position - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return (backslashes & 1) == 1;
    }

    private boolean containsLineBreak(String text)
    {
        return text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
    }
}
