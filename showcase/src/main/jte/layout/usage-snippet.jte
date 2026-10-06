@param String code
@param String label = "Usage"

<%-- A copyable JTE example, reusing the one pre-existing `<pre>` convention
     in this codebase (pages/components/form-field.jte's "wiring contract"
     block) rather than inventing new styling.

     `code` MUST be built as a Java String (ideally a text block) in a
     `!{...}` block at the call site, never written as literal template
     markup containing `@template...` text. JTE's directive scanner only
     ever looks at a file's own template markup for `@` - a Java string
     literal's contents are invisible to it, so a real `@template.ashlar...`
     example lives safely inside the string and can never be mistaken for an
     actual nested call. The homepage work that came before this utility
     hand-escaped `@` as `&#64;` directly in markup instead, and a single
     missed escape there once caused a snippet to execute as a live
     component rather than print as text - building `code` as a String
     removes that failure mode entirely rather than relying on every caller
     remembering to escape it correctly.

     `${code}` relies on JTE's own default HTML-escaping of `${}` output
     (already standard throughout this codebase) to keep generics like
     `List<DataTableColumn>` displaying correctly rather than being parsed
     as tags.

     ONE MORE CAVEAT, FOUND WRITING THE DATA TABLE PAGE: JTE's raw-content
     scanner looks for its own open token - the at-sign character
     immediately followed by a backtick, no space between - anywhere in a
     file's raw source text, even inside a Java string literal inside a
     code block. It does not understand Java-string context at all: it just
     found what looks like its own open token, so it started a real nested
     content block there and kept consuming subsequent file content as that
     block's markup (meaning real template calls after it actually ran)
     until the next bare backtick, which is nowhere near where the Java
     string's own logical end is. That leaked raw gg.jte.html.HtmlContent
     internals into the rendered page before this note existed.
     Consequence: a snippet's source text must never contain that exact
     two-character open token, and should avoid an unpaired closing
     backtick too. A snippet that genuinely needs to illustrate that syntax
     has to build it from concatenated pieces - keep the at-sign and the
     backtick apart in the file's own source, and assemble the real
     character sequence only at render time (for example, inserting a
     separately-declared one-backtick String between two text-block halves
     that never touch it directly) so the dangerous token never appears
     in the .jte file itself.

     Do not try to work around this by typing a Java unicode escape for the
     character in place of the glyph itself, hoping the file's own raw text
     stays free of it until Java evaluates the escape at compile time: an
     editing tool can silently resolve that escape into the real character
     before the file is even written, which defeats the whole point and
     reproduces this exact bug. The verified-safe pattern is structural, not
     a spelling trick - keep the character isolated on its own line as a
     complete, already-closed string literal assigned to its own variable
     (as both the Data Table and Modal pages do), so the at-sign in the
     surrounding text block never sits in the same literal as it, and
     splice the two together with ordinary Java string concatenation. --%>
!{boolean hasLabel = !label.isEmpty();}
<div>
    @if (hasLabel)
        <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">${label}</h3>
    @endif
    <pre class="mt-2 overflow-x-auto rounded bg-slate-900 p-3 text-xs text-slate-100 dark:bg-slate-950"><code>${code}</code></pre>
</div>
