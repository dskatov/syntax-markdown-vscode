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
package org.xwiki.contrib.rendering.markdown.commonmark12.internal;

import java.io.StringReader;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.DefaultFlexmarkNodeVisitor;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12Parser;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12StreamParser;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.internal.parser.plain.PlainTextStreamParser;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRendererFactory;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.junit.Assert.assertEquals;

/**
 * Verify that 2 spaces at end of line are transformed into a new line when the newline separator is either
 * {@code 
} or {@code 
}.
 *
 * @version $Id$
 * @since 8.4.3
 */
@ComponentList({
    Markdown12Parser.class,
    Markdown12StreamParser.class,
    DefaultFlexmarkNodeVisitor.class,
    PlainTextStreamParser.class,
    PlainTextRendererFactory.class
})
public class Markdown12SpaceIntoNewlineTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @BeforeComponent
    public void setUpComponents() throws Exception
    {
        // Not needed for the test so we just mock them
        this.mocker.registerMockComponent(MarkdownConfiguration.class);
        this.mocker.registerMockComponent(ResourceReferenceParser.class, "image");
        this.mocker.registerMockComponent(ResourceReferenceParser.class, "link");
    }

    @Test
    public void convertTwoSpacesIntoNewMine() throws Exception
    {
        Parser parser = this.mocker.getInstance(Parser.class, "markdown/1.2");

        XDOM xdom = parser.parse(new StringReader("paragraph1 on  \nmultiple  \nlines\n"));
        assertEquals(2, xdom.getBlocks(new ClassBlockMatcher(NewLineBlock.class), Block.Axes.DESCENDANT).size());

        xdom = parser.parse(new StringReader("paragraph1 on  \r\nmultiple  \r\nlines\r\n"));
        assertEquals(2, xdom.getBlocks(new ClassBlockMatcher(NewLineBlock.class), Block.Axes.DESCENDANT).size());
    }
}
