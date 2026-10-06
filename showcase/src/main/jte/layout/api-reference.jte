@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param List<ApiParam> apiParams

<%-- The public, JTE-facing @param contract for one component - built from
     ApiParam.java, not reflection or a schema: the repository's actual
     @param lists are the source of truth, and this table is a hand-written
     transcription of them (verified against the component's own .jte file
     at doc-writing time), the same way every other piece of prose on these
     pages is hand-written from reading the source rather than generated.

     Named `apiParams`, not `params`: JTE's own generated renderMap() method
     already declares a local variable literally called `params` (its
     internal map of call-site arguments) - a @param also named `params`
     collides with it and fails to compile. Worth remembering for every
     future partial, not just this one. --%>
<div class="overflow-x-auto">
    <table class="w-full border-collapse text-left text-sm">
        <thead>
            <tr class="border-b border-slate-200 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-700 dark:text-slate-400">
                <th class="py-2 pr-4">Param</th>
                <th class="py-2 pr-4">Type</th>
                <th class="py-2 pr-4">Required</th>
                <th class="py-2 pr-4">Default</th>
                <th class="py-2">Description</th>
            </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
            @for (ApiParam param : apiParams)
                <tr>
                    <td class="py-2 pr-4 font-mono text-xs text-slate-900 dark:text-slate-100">${param.name()}</td>
                    <td class="py-2 pr-4 font-mono text-xs text-slate-500 dark:text-slate-400">${param.type()}</td>
                    <td class="py-2 pr-4 text-slate-500 dark:text-slate-400">${param.required() ? "Required" : "Optional"}</td>
                    <td class="py-2 pr-4 font-mono text-xs text-slate-500 dark:text-slate-400">${param.required() ? "—" : (param.defaultValue().isEmpty() ? "\"\"" : param.defaultValue())}</td>
                    <td class="py-2 text-slate-700 dark:text-slate-300">${param.description()}</td>
                </tr>
            @endfor
        </tbody>
    </table>
</div>
