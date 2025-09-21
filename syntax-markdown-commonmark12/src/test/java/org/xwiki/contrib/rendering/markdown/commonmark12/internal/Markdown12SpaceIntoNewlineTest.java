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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.DefaultFlexmarkNodeVisitor;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12Parser;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12ParserCompat;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12StreamParser;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12StreamParserCompat;
import org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.DefaultFlexmarkNodeVisitor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

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
        Markdown12ParserCompat.class,
        Markdown12StreamParser.class,
        Markdown12StreamParserCompat.class,
        DefaultFlexmarkNodeVisitor.class
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
        registerPlainComponents();
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

    private void registerPlainComponents() throws Exception
    {
        StreamParser plainStreamParser = this.mocker.registerMockComponent(StreamParser.class, "plain/1.0");
        Mockito.doAnswer(invocation -> {
            Reader reader = invocation.getArgument(0, Reader.class);
            Listener listener = invocation.getArgument(1, Listener.class);
            parsePlain(reader, listener);
            return null;
        }).when(plainStreamParser).parse(any(Reader.class), any(Listener.class));

        PrintRendererFactory plainRendererFactory =
            this.mocker.registerMockComponent(PrintRendererFactory.class, "plain/1.0");
        Mockito.doAnswer(invocation -> {
            WikiPrinter printer = invocation.getArgument(0, WikiPrinter.class);
            PrintRenderer renderer = Mockito.mock(PrintRenderer.class);
            Mockito.when(renderer.getPrinter()).thenReturn(printer);
            return renderer;
        }).when(plainRendererFactory).createRenderer(any(WikiPrinter.class));
    }

    private void parsePlain(Reader reader, Listener listener) throws IOException
    {
        StringBuilder word = new StringBuilder();
        int value;
        while ((value = reader.read()) != -1) {
            char character = (char) value;
            if (character == '\r') {
                continue;
            }
            if (character == '\n') {
                flushWord(word, listener);
                listener.onNewLine();
            } else if (Character.isWhitespace(character)) {
                flushWord(word, listener);
                listener.onSpace();
            } else {
                word.append(character);
            }
        }
        flushWord(word, listener);
    }

    private void flushWord(StringBuilder buffer, Listener listener)
    {
        if (buffer.length() > 0) {
            listener.onWord(buffer.toString());
            buffer.setLength(0);
        }
    }
}
