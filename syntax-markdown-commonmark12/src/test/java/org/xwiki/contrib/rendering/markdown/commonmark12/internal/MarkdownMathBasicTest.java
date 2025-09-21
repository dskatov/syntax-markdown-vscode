/*
 * Basic tests for markdown-math/1.0 inline and block math.
 */
package org.xwiki.contrib.rendering.markdown.commonmark12.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void mathInsideListItemsIsDetected() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "markdown-math/1.0");
        String sample = "Given data of $x$:\n\n- Optimize flow:\n  $$ x^2 + y^2 = z^2 $$\n"
            + "- Subsidize point:   $$ 4 + 5 = 9 $$\n";
        XDOM xdom = parser.parse(new StringReader(sample));

        List<MacroBlock> macros = findMathMacros(xdom);
        List<MacroBlock> inlineMacros = new ArrayList<>();
        List<MacroBlock> blockMacros = new ArrayList<>();
        for (MacroBlock macro : macros) {
            if (macro.isInline()) {
                inlineMacros.add(macro);
            } else {
                blockMacros.add(macro);
            }
        }

        assertEquals("Expected inline math span for $x$", 1, inlineMacros.size());
        assertEquals("\\(x\\)", inlineMacros.get(0).getContent());

        assertEquals("Expected two block math macros", 2, blockMacros.size());
        assertEquals("\\[\nx^2 + y^2 = z^2\n\\]", blockMacros.get(0).getContent());
        assertEquals("\\[\n4 + 5 = 9\n\\]", blockMacros.get(1).getContent());
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

    private List<MacroBlock> findMathMacros(Block block)
    {
        List<MacroBlock> macros = new ArrayList<>();
        if (block instanceof MacroBlock) {
            MacroBlock macroBlock = (MacroBlock) block;
            if ("mathjax".equals(macroBlock.getId())) {
                macros.add(macroBlock);
            }
        }

        for (Block child : block.getChildren()) {
            macros.addAll(findMathMacros(child));
        }

        return macros;
    }

    private List<MacroBlock> findMathMacros(XDOM xdom)
    {
        List<MacroBlock> macros = new ArrayList<>();
        for (Block block : xdom.getChildren()) {
            macros.addAll(findMathMacros(block));
        }
        return macros;
    }

}
