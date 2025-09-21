package org.xwiki.contrib.rendering.markdown.commonmark12.internal.renderer;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

/**
 * Compatibility renderer factory published under the legacy {@code markdown/1.2} hint.
 */
@Component
@Named("markdown/1.2")
@Singleton
public class Markdown12RendererFactoryCompat extends Markdown12RendererFactory
{
    private static final Syntax MARKDOWN_1_2 = new Syntax(SyntaxType.MARKDOWN, "1.2");

    @Override
    public Syntax getSyntax()
    {
        return MARKDOWN_1_2;
    }
}