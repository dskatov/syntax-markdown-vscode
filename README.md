# Markdown Syntaxes

Parsers and Renderers for Markdown syntaxes.

* Project Lead: [Vincent Massol](https://www.xwiki.org/xwiki/bin/view/XWiki/VincentMassol)
* [Documentation & Downloads](https://extensions.xwiki.org/xwiki/bin/view/Extension/Markdown%20Syntax%201.2)
* [Issue Tracker](https://jira.xwiki.org/browse/MARKDOWN)
* Communication: [Forum](https://forum.xwiki.org), [Chat](https://dev.xwiki.org/xwiki/bin/view/Community/Chat)
* [Development Practices](https://dev.xwiki.org)
* Minimal XWiki version supported: XWiki 12.10
* License: LGPL 2.1
* Translations: N/A
* Sonar Dashboard: N/A
* Continuous Integration Status: [![Build Status](http://ci.xwiki.org/job/XWiki%20Contrib/job/syntax-markdown/job/master/badge/icon)](http://ci.xwiki.org/job/XWiki%20Contrib/job/syntax-markdown/job/master/)

## Math Support (CommonMark 1.2)

- Inline math: `$...$` is converted to an inline macro using the macro id from `xwiki.markdownmath10.math.macro` (default: `mathjax`).
- Block math: paragraphs that start with `$$` and end with `$$` are converted to a standalone macro block with the same macro id.
- Inside `$...$`/`$$...$$`, no markdown emphasis or GFM features are parsed; the content is passed verbatim to the macro.
- Superscript, subscript, and GFM strikethrough Flexmark extensions are not enabled in CommonMark 1.2.

Configure the macro id globally with a system property:

- `-Dxwiki.markdownmath10.math.macro=mathjax` (default) or `formula`.

To render math on pages, install the corresponding macro extension in XWiki:

- MathJax Macro (client-side) or Formula Macro (server-side).

## Build & Install (Markdown Math 1.0)

- Build: `mvn -DskipTests -DskipITs clean package`
- Install farm-wide: upload the produced JAR(s) from `syntax-markdown-commonmark12/target/` with Extension Manager (Advanced -> Upload) or drop them in `<xwiki>/WEB-INF/lib` and restart.
- New syntax id: `markdown-math/1.0` (does not clash with stock `markdown/1.2`).

---

# Markdown Math 1.0 - Implementation Journal & Ops Guide

This document captures the end-to-end work done to add LaTeX math to XWiki's Markdown parser while preventing Markdown emphasis/sub/sup/strike inside math, and packaging it for safe drop-in use with the official Docker image (Tomcat + Postgres).

## Goals

- Add `$...$` (inline) and `$$...$$` (block) math to Markdown rendering.
- Ensure characters like `^`, `_`, `~`, `~~` inside math are never parsed as Markdown formatting.
- Keep everything else working: links, images, tables, abbreviations, etc.
- Avoid clashing with the stock Markdown 1.2 extension; offer a new syntax id.
- Provide a single JAR that can be bind-mounted into the Docker image.

## What We Built

- A new syntax id: `markdown-math/1.0` with math support.
- A compatibility registration so `markdown/1.2` still resolves to a working parser even if our JAR is present (see "Compatibility" below).
- A shaded JAR that bundles Flexmark libs and Autolink and relocates them to avoid conflicts in XWiki.

### Modules and Files

- Core code changes live in:
  - `syntax-markdown/syntax-markdown-commonmark12/src/main/java/...` (parsers, visitors, config)
  - `syntax-markdown/syntax-markdown-commonmark12/pom.xml` (dependencies, shading, Java level)
- Deployable JAR placed at:
  - `deploy/syntax-markdown-math10-16.5.0-math1.0.jar`

## Behavior Details

- Inline math `$...$` -> emitted as an inline macro (default `{{mathjax}}...{{/mathjax}}`).
- Block math `$$...$$` -> emitted as a standalone macro block.
- Inside math, no Markdown formatting is applied (raw LaTeX goes to the macro).
- Outside math, emphasis/strong remain available; superscript/subscript/strike are not enabled.
- Display math inside list items or sharing a line with descriptive text still becomes a block macro.

### Macro selection

- System property (unique key in this fork):
  - `-Dxwiki.markdownmath10.math.macro=mathjax` (default) or `formula`

## Compatibility

- New syntax id: `markdown-math/1.0` (non-clashing).
- Compatibility components register the original hint so legacy pages work:
  - `org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12ParserCompat` -> `@Named("markdown/1.2")`
  - `org.xwiki.contrib.rendering.markdown.commonmark12.internal.parser.Markdown12StreamParserCompat` -> `@Named("markdown/1.2")`
- This prevents "MissingParserException: markdown/1.2" when the stock extension is absent or overshadowed.

## Packaging Strategy (Shaded JAR)

- Shaded and relocated libraries inside the JAR:
  - Flexmark (all modules) -> `org.xwiki.contrib.markdown.math.flexmark`
  - Autolink (org.nibor.autolink) -> `org.xwiki.contrib.markdown.math.autolink`
- XWiki classes are not shaded (provided by platform); our classes remain under `org.xwiki.contrib.*` in the JAR.

## Repro Steps / Commands

### Build locally (module only)

- Build just the CommonMark 1.2 module (skip tests):
  - Windows/PowerShell: `mvn -f syntax-markdown/syntax-markdown-commonmark12/pom.xml -DskipTests clean package`

### Deploy in Docker

- Bind mount the JAR (compose excerpt):
  - `"./deploy/syntax-markdown-math10-16.5.0-math1.0.jar:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/syntax-markdown-math10-16.5.0-math1.0.jar:ro"`
- Restart only XWiki/Tomcat (no DB):
  - `docker compose restart web`
  - Or recreate for a clean redeploy: `docker compose up -d --force-recreate web`
- Follow logs:
  - `docker logs -f --tail=300 xwiki-postgres-tomcat-web`
- Optional: clear Tomcat work cache prior to restart:
  - `docker exec xwiki-postgres-tomcat-web sh -lc 'rm -rf /usr/local/tomcat/work/Catalina/localhost/*'`

## Verification Checklist

- Admin -> Content -> Rendering -> ensure `markdown-math/1.0` is enabled.
- Create a page using `markdown-math/1.0` and paste:
  - Inline: `Let $E = mc^2`.
  - Block:
    ```
    $$
    a^2 + b^2 = c^2
    $$
    ```
- Optional list sample:
  ```
  - Optimize flow:
    $$ x^2 + y^2 = z^2 $$
  - Subsidize point:   $$ 4 + 5 = 9 $$
  ```
- Expected: math renders via MathJax (or Formula), emphasis outside math still works, nothing inside math is bold/italic/strike.

## Troubleshooting (Observed and Fixed)

- Symptom: HTTP 404 after adding JAR
  - Root cause: webapp (ROOT) failed to deploy due to missing classes (earlier shaded JAR accidentally excluded our org/xwiki/** classes).
  - Fix: rebuilt the JAR without excluding `org/xwiki/**`; confirmed via Tomcat logs (`localhost.YYYY-MM-DD.log`).

- Symptom: MissingParserException for `markdown/1.2`
  - Root cause: stock Markdown 1.2 components overshadowed/absent; our JAR had a new syntax id only.
  - Fix: added compatibility components registering `@Named("markdown/1.2")` so legacy pages still parse.

- Symptom: NoClassDefFoundError: `org/nibor/autolink/LinkType`
  - Root cause: Flexmark autolink depends on autolink library which wasn't shaded.
  - Fix: added dependency `org.nibor.autolink:autolink:0.6.0` and shaded/relocated it into the JAR.

## Tests

- Minimal unit testing added to validate math blocks and spans produce macro blocks, including inline, blank-line, and list-item display math scenarios (see module's MarkdownMathBasicTest).
- Run the focused regression suite locally with mvn -pl syntax-markdown-commonmark12 -am -Dtest=MarkdownMathBasicTest test.

## Current State (Working)

- Pages render again (no 404; no MissingParserException for `markdown/1.2`).
- New syntax `markdown-math/1.0` available and functioning.
- Autolink NoClassDefFoundError resolved (autolink shaded).
- Deployable JAR prepared at `deploy/syntax-markdown-math10-16.5.0-math1.0.jar`.

## Notes & Limits

- This fork focuses on Markdown parsing; it does not bundle MathJax or Formula macros - install one of them via Extension Manager.
- If you also have the stock "CommonMark Markdown Syntax 1.2" extension installed, both can coexist; the compatibility components ensure `markdown/1.2` resolves even if our JAR is present.
- Java 17 is required (aligned with XWiki 16.x).

## Next Steps (Where We Stopped)

- Verify escaping around math delimiters (e.g., `\$` outside math, edge cases with multiple `$`).
- Add unit tests for block/inline math mixed with links, images, and code spans.
- Decide and document the final default macro (`mathjax` vs `formula`) and keep `xwiki.markdownmath10.math.macro` as the single source of truth.
- Consider publishing the shaded JAR as a GitHub Release and attaching deploy notes.
- Optional: upstream a proposal/PR to xwiki-contrib if we want this syntax id maintained officially.

## Quick Commands (for ops)

- Restart only XWiki web container: `docker compose restart web`
- Recreate web container (clean deploy): `docker compose up -d --force-recreate web`
- Tail logs: `docker logs -f --tail=300 xwiki-postgres-tomcat-web`
## Development Status (September 2025)

- Reintroduced `MarkdownMathBasicTest` to lock inline `$...$` and block `$$...$$` parsing; run with `mvn -pl syntax-markdown-commonmark12 -Dtest=MarkdownMathBasicTest test -DskipITs`.
- Added compatibility renderer components for the legacy `markdown/1.2` hint so the math-aware stack can be reused.
- Test fixtures under `src/test/resources/markdown12/specific` now target `markdown-math/1.0` and include the sample math page.
- `mvn -pl syntax-markdown-commonmark12 -am test -DskipITs` still fails: math expectations in `markdown12/specific/math.test` and related subscript/superscript cases are not satisfied yet, and configuration tests lack real plain renderer bindings.


