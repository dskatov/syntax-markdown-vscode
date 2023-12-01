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

import org.xwiki.rendering.listener.Listener;

import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.ast.HtmlEntity;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.HtmlInlineComment;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;

/**
 * Handle HTML events.
 *
 * @version $Id$
 * @since 8.4
 */
public class HTMLNodeVisitor extends AbstractNodeVisitor
{
    static <V extends HTMLNodeVisitor> VisitHandler<?>[] VISIT_HANDLERS(final V visitor)
    {
        return new VisitHandler<?>[]{
                new VisitHandler<>(HtmlInline.class, node -> visitor.visit(node)),
                new VisitHandler<>(HtmlBlock.class, node -> visitor.visit(node)),
                new VisitHandler<>(HtmlCommentBlock.class, node -> visitor.visit(node)),
                new VisitHandler<>(HtmlEntity.class, node -> visitor.visit(node)),
                new VisitHandler<>(HtmlInlineComment.class, node -> visitor.visit(node))
        };
    }

    public HTMLNodeVisitor(NodeVisitor visitor, Deque<Listener> listeners)
    {
        super(visitor, listeners);
    }

    public void visit(HtmlInline node)
    {
        visit((Node) node);
    }

    public void visit(HtmlBlock node)
    {
        // Flexmark puts trailing newline in the HTML block so we need to remove it.
        String html = node.getChars().toString().trim();
        generateHTMLMacro(html, false);
    }

    public void visit(HtmlCommentBlock node)
    {
        visit((Node) node);
    }

    public void visit(HtmlEntity node)
    {
        visit((Node) node);
    }

    public void visit(HtmlInlineComment node)
    {
        visit((Node) node);
    }

    private void visit(Node node)
    {
        generateHTMLMacro(node.getChars().toString(), true);
    }
}
