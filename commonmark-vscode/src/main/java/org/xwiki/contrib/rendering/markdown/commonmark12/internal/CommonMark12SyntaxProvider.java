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

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;

/**
 * Register the {@code commonmark-vscode/0.1} Syntax supported by this module.
 *
 * @version $Id$
 * @since 8.8
 */
@Component
@Named("commonmark-vscode/0.1")
@Singleton
public class CommonMark12SyntaxProvider implements Provider<List<Syntax>>
{
    /**
     * CommonMark VSCode Markdown syntax type.
     */
    public static final SyntaxType COMMONMARK_VSCODE = new SyntaxType("commonmark-vscode", "CommonMark VSCode Markdown");

    /**
     * CommonMark VSCode Markdown 0.1 syntax.
     */
    public static final Syntax COMMONMARK_VSCODE_0_1 = new Syntax(COMMONMARK_VSCODE, "0.1");

    @Override
    public List<Syntax> get()
    {
        return Collections.singletonList(COMMONMARK_VSCODE_0_1);
    }
}
