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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

/**
 * Compatibility parser registering the original hint {@code markdown/1.2}
 * to ensure existing pages and defaults continue to work when this module is present.
 */
@Component
@Named("markdown/1.2")
@Singleton
public class Markdown12ParserCompat extends AbstractMarkdownParser
{
    @Inject
    @Named("markdown/1.2")
    private StreamParser compatStreamParser;

    @Override
    protected StreamParser getMarkdownStreamParser()
    {
        return this.compatStreamParser;
    }

    @Override
    public Syntax getSyntax()
    {
        return new Syntax(new SyntaxType("markdown", "CommonMark Markdown"), "1.2");
    }
}

