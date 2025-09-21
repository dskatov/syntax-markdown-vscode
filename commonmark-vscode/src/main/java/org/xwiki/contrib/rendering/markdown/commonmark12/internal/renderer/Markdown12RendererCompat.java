package org.xwiki.contrib.rendering.markdown.commonmark12.internal.renderer;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

/**
 * Compatibility renderer registered under the {@code markdown/1.2} hint.
 */
@Component
@Named("markdown/1.2")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class Markdown12RendererCompat extends Markdown12Renderer
{
}