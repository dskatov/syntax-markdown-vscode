/*
 * Basic tests for commonmark-vscode/0.1 inline and block math.
 */
package org.xwiki.contrib.rendering.markdown.commonmark12.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private static final char BACKSLASH = '\\';

    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Test
    public void inlineMathEmitsWrappedMacro() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "commonmark-vscode/0.1");
        XDOM xdom = parser.parse(new StringReader("Inline $E = mc^2$ here."));

        String content = findMacroContent(xdom, true);
        assertNotNull("Inline math macro not found", content);
        assertEquals(inlineMath("E = mc^2"), content);
    }

    @Test
    public void blockMathEmitsWrappedMacro() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "commonmark-vscode/0.1");
        String blockSample = String.join("\n",
                "$$",
                "A^2 + B^2 = C^2",
                "$$",
                "");

        XDOM xdom = parser.parse(new StringReader(blockSample));

        String content = findMacroContent(xdom, false);
        assertNotNull("Block math macro not found", content);
        assertEquals(blockMath("A^2 + B^2 = C^2"), content);
    }

    @Test
    public void blockMathWithOperatorNameAndInlineContext() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "commonmark-vscode/0.1");
        String expression = "\\xi^\\text{sprong}_r = \\operatorname{whurl}\\left(\\{n_{u} - n_{u-1}\\}_{u=r-h}^{r}\\right)";
        String sample = String.join("\n",
                "Each shiver start index $r$ leans on a pre-jitter spool gathered before gulping $y_r$ (no hindsight).",
                "$$",
                expression,
                "$$",
                "That routine samples the cache at the exact same tick.");

        XDOM xdom = parser.parse(new StringReader(sample));

        List<MacroBlock> macros = findMathMacros(xdom);
        List<MacroBlock> blockMacros = new ArrayList<>();
        for (MacroBlock macro : macros) {
            if (!macro.isInline()) {
                blockMacros.add(macro);
            }
        }

        assertEquals("Expected a block math macro for the $$ expression", 1, blockMacros.size());
        assertEquals(blockMath(expression), blockMacros.get(0).getContent());
    }

    @Test
    public void inlineMathWithUnderscoresSurvivesEmphasis() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "commonmark-vscode/0.1");
        String expression = "\\upsilon^\\text{glint}_j = \\theta_{r(j)}";
        String sample = String.join(" ",
                "Nebulae at pass $j$ cling to a wobble gauge, so the stitched readout",
                "$" + expression + "$",
                "stays glued in place.");

        XDOM xdom = parser.parse(new StringReader(sample));

        List<MacroBlock> inlineMacros = findMathMacros(xdom).stream()
            .filter(MacroBlock::isInline)
            .collect(Collectors.toList());

        boolean found = inlineMacros.stream()
            .map(MacroBlock::getContent)
            .anyMatch(inlineMath(expression)::equals);
        assertTrue("Inline math placeholder lost expected content", found);
    }

    @Test
    public void mathInsideListItemsIsDetected() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "commonmark-vscode/0.1");
        String listSample = String.join("\n",
                "Given data of $x$:",
                "",
                "- Optimize flow:",
                "  $$ x^2 + y^2 = z^2 $$",
                "- Subsidize point:   $$ 4 + 5 = 9 $$",
                "");

        XDOM xdom = parser.parse(new StringReader(listSample));

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
        assertEquals(inlineMath("x"), inlineMacros.get(0).getContent());

        assertEquals("Expected two block math macros", 2, blockMacros.size());
        assertEquals(blockMath("x^2 + y^2 = z^2"), blockMacros.get(0).getContent());
        assertEquals(blockMath("4 + 5 = 9"), blockMacros.get(1).getContent());
    }

    @Test
    public void complexPageDoesNotDuplicateContent() throws Exception
    {
        registerMinimalMocks();

        Parser parser = this.mocker.getInstance(Parser.class, "commonmark-vscode/0.1");
        String complexSample = String.join("\n",
                "Click on **\"Edit\"** and modify the contents of this page, then click on **\"Save & View\"** to see how they look like on your page!",
                "",
                "# Here's some dummy text to show you what the page looks like",
                "",
                "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                "",
                "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                "",
                "Inline: $ x^{\text{pre}}_t $.",
                "",
                "$$",
                "\\sum_{i=1}^{n} x_i^2",
                "$$",
                "",
                "{{mathjax}}",
                "\\begin{equation}",
                "y = \\frac{c}{cb-ad}",
                "\\end{equation}",
                "{{/mathjax}}",
                "",
                "$K_t = \\tfrac12 v_t^2$",
                "",
                "$$",
                "\\[",
                "\\begin{equation}",
                "y = \\frac{c}{cb-ad}",
                "\\end{equation}",
                "\\]",
                "$$",
                "",
                "Given data of $x$:",
                "",
                "- Optimize flow:",
                "  $$ x^2 + y^2 = z^2 $$",
                "- Subsidize point:   $$ 4 + 5 = 9 $$",
                "");

        XDOM xdom = parser.parse(new StringReader(complexSample));

        List<String> paragraphs = collectParagraphTexts(xdom);
        long inlineCount = paragraphs.stream().filter(p -> p.contains("Inline:")).count();
        long givenCount = paragraphs.stream().filter(p -> p.contains("Given data of")).count();

        assertEquals("Inline label duplicated", 1, inlineCount);
        assertEquals("Given label duplicated", 1, givenCount);
    }

    private String inlineMath(String body)
    {
        return String.valueOf(BACKSLASH) + '(' + body + BACKSLASH + ')';
    }

    private String blockMath(String body)
    {
        return String.valueOf(BACKSLASH) + '[' + '\n' + body + '\n' + BACKSLASH + ']';
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

    private void registerMinimalMocks() throws Exception
    {
        org.xwiki.rendering.parser.StreamParser plainStreamParser =
            this.mocker.registerMockComponent(org.xwiki.rendering.parser.StreamParser.class, "plain/1.0");
        Mockito.doAnswer(invocation -> {
            Reader reader = invocation.getArgument(0, Reader.class);
            Listener listener = invocation.getArgument(1, Listener.class);
            try {
                parsePlain(reader, listener);
            } catch (IOException exception) {
                throw new RuntimeException("Failed to parse plain text", exception);
            }
            return null;
        }).when(plainStreamParser).parse(Mockito.any(Reader.class), Mockito.any(Listener.class));

        PrintRendererFactory printRendererFactory =
                this.mocker.registerMockComponent(PrintRendererFactory.class, "plain/1.0");
        when(printRendererFactory.createRenderer(any(WikiPrinter.class))).thenAnswer(invocation -> {
            WikiPrinter printer = invocation.getArgument(0, WikiPrinter.class);
            PrintRenderer renderer = mock(PrintRenderer.class);
            when(renderer.getPrinter()).thenReturn(printer);
            return renderer;
        });

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

    private List<String> collectParagraphTexts(XDOM xdom)
    {
        List<String> paragraphs = new ArrayList<>();
        for (Block block : xdom.getChildren()) {
            collectParagraphTexts(block, paragraphs);
        }
        return paragraphs;
    }

    private void collectParagraphTexts(Block block, List<String> paragraphs)
    {
        if (block instanceof ParagraphBlock) {
            paragraphs.add(extractText(block));
        }
        for (Block child : block.getChildren()) {
            collectParagraphTexts(child, paragraphs);
        }
    }

    private String extractText(Block block)
    {
        StringBuilder builder = new StringBuilder();
        if (block instanceof WordBlock) {
            builder.append(((WordBlock) block).getWord());
        } else if (block instanceof SpaceBlock) {
            builder.append(' ');
        } else if (block instanceof SpecialSymbolBlock) {
            builder.append(((SpecialSymbolBlock) block).getSymbol());
        } else if (block instanceof NewLineBlock) {
            builder.append('\n');
        }
        for (Block child : block.getChildren()) {
            builder.append(extractText(child));
        }
        return builder.toString();
    }
}

