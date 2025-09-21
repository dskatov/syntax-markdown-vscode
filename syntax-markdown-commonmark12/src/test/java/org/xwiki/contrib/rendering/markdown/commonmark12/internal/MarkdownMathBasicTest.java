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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ComponentList({
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12Parser.class,
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12StreamParser.class,
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12ParserCompat.class,
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12StreamParserCompat.class,
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.DefaultFlexmarkNodeVisitor.class,
    org.xwiki.contrib.rendering.markdown.commonmark12.internal.DefaultMarkdownConfiguration.class
})
public class MarkdownMathBasicTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Test
    public void inlineMathEmitsWrappedMacro() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "markdown-math/1.0");
        XDOM xdom = parser.parse(new StringReader("Inline $E = mc^2$ here."));

        String content = findMacroContent(xdom, true);
        assertNotNull("Inline math macro not found", content);
        assertEquals("\\(E = mc^2\\)", content);
    }

    @Test
    public void blockMathEmitsWrappedMacro() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "markdown-math/1.0");
        XDOM xdom = parser.parse(new StringReader("$$\nA^2 + B^2 = C^2\n$$"));

        String content = findMacroContent(xdom, false);
        assertNotNull("Block math macro not found", content);
        assertEquals("\\[\nA^2 + B^2 = C^2\n\\]", content);
    }

    @Test
    public void blockMathWithBlankLinesIsDetected() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "markdown-math/1.0");
        String sample = "$$\n\n\n\\begin{equation}\ny = \\frac{c}{cb-ad}\n\\end{equation}\n\n\n$$";
        XDOM xdom = parser.parse(new StringReader(sample));

        String content = findMacroContent(xdom, false);
        assertNotNull("Block math macro not found for blank-line sample", content);
        assertEquals("\\[\n\\begin{equation}\ny = \\frac{c}{cb-ad}\n\\end{equation}\n\\]", content);
    }

    private void registerMinimalMocks() throws Exception
    {
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.StreamParser.class, "plain/1.0");
        this.mocker.registerMockComponent(org.xwiki.rendering.renderer.PrintRendererFactory.class, "plain/1.0");
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.ResourceReferenceParser.class, "image");
        this.mocker.registerMockComponent(org.xwiki.rendering.parser.ResourceReferenceParser.class, "link");
    }

    private String findMacroContent(Block block, boolean inline)
    {
        if (block instanceof MacroBlock) {
            MacroBlock macroBlock = (MacroBlock) block;
            if ("mathjax".equals(macroBlock.getId()) && macroBlock.isInline() == inline) {
                return macroBlock.getContent();
            }
        }

        for (Block child : block.getChildren()) {
            String result = findMacroContent(child, inline);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private String findMacroContent(XDOM xdom, boolean inline)
    {
        for (Block block : xdom.getChildren()) {
            String result = findMacroContent(block, inline);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
