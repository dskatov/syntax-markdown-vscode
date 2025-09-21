/*
 * Basic tests for markdown-math/1.0 inline and block math.
 */
package org.xwiki.contrib.rendering.markdown.commonmark12.internal;

import java.io.StringReader;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.junit.Assert.assertTrue;

@ComponentList({
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12Parser.class,
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12StreamParser.class,
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.DefaultFlexmarkNodeVisitor.class,
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.DefaultMarkdownConfiguration.class
})
public class MarkdownMathBasicTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Test
    public void inlineMathEmitsMacro() throws Exception
    {
        // Register minimal mocks for dependencies not exercised by this test
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.StreamParser.class, "plain/1.0");
        this.mocker.registerMockComponent(org.xwiki.rendering.renderer.PrintRendererFactory.class, "plain/1.0");
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.ResourceReferenceParser.class, "image");
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.ResourceReferenceParser.class, "link");

        Parser parser = this.mocker.getInstance(Parser.class, "markdown-math/1.0");
        XDOM xdom = parser.parse(new StringReader("Inline $E = mc^2$ here."));

        assertTrue(containsMacro(xdom, true, "E = mc^2"));
    }

    @Test
    public void blockMathEmitsMacro() throws Exception
    {
        // Register minimal mocks for dependencies not exercised by this test
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.StreamParser.class, "plain/1.0");
        this.mocker.registerMockComponent(org.xwiki.rendering.renderer.PrintRendererFactory.class, "plain/1.0");
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.ResourceReferenceParser.class, "image");
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.ResourceReferenceParser.class, "link");

        Parser parser = this.mocker.getInstance(Parser.class, "markdown-math/1.0");
        XDOM xdom = parser.parse(new StringReader("\n$$\nA^2 + B^2 = C^2\n$$\n"));

        assertTrue(containsMacro(xdom, false, "A^2 + B^2 = C^2"));
    }

    private boolean containsMacro(Block block, boolean inline, String contains)
    {
        if (block instanceof MacroBlock) {
            MacroBlock mb = (MacroBlock) block;
            return "mathjax".equals(mb.getId()) && mb.isInline() == inline
                && mb.getContent() != null && mb.getContent().contains(contains);
        }
        for (Block child : block.getChildren()) {
            if (containsMacro(child, inline, contains)) {
                return true;
            }
        }
        return false;
    }
}
