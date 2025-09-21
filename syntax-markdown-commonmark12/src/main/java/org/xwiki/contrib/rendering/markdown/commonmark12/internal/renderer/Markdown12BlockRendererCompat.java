package org.xwiki.contrib.rendering.markdown.commonmark12.internal.renderer;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Compatibility block renderer exposed under the legacy {@code markdown/1.2} hint.
 */
@Component
@Named("markdown/1.2")
@Singleton
public class Markdown12BlockRendererCompat extends Markdown12BlockRenderer
{
}