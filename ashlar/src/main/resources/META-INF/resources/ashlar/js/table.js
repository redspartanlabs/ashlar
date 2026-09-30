// Sorting, filtering and paging for table.jte. Every one of those operates
// on the rows that are already in the DOM - the server rendered them, this
// file only reorders and hides them. It never fetches anything, and it never
// changes a cell's content.
//
// Same shape as the rest of the library's JavaScript: one init function per
// [data-table] root, all state closed over inside it, so several tables on a
// page never see each other.

function parseNumber(value) {
    // Tolerant on purpose: callers hand over display strings like "1,204,981",
    // "$128.40" or "37%", and a column marked `number` means "compare these as
    // numbers" rather than "these are bare digits".
    const cleaned = value.replace(/[^0-9.eE+-]/g, "");
    const parsed = Number.parseFloat(cleaned);
    return Number.isNaN(parsed) ? null : parsed;
}

function parseDate(value) {
    const parsed = Date.parse(value);
    return Number.isNaN(parsed) ? null : parsed;
}

function initTable(root) {
    const body = root.querySelector("[data-table-body]");
    if (!body) {
        return;
    }

    const headings = Array.from(root.querySelectorAll("[data-table-column]"));
    const sortButtons = Array.from(root.querySelectorAll("[data-table-sort]"));
    const search = root.querySelector("[data-table-search]");
    const status = root.querySelector("[data-table-status]");
    const pageIndicator = root.querySelector("[data-table-page-indicator]");
    const previous = root.querySelector("[data-table-prev]");
    const next = root.querySelector("[data-table-next]");
    const scroll = root.querySelector("[data-table-scroll]");
    const emptyNone = root.querySelector('[data-table-empty="none"]');
    const emptyFiltered = root.querySelector('[data-table-empty="filtered"]');

    const selectAll = root.querySelector("[data-table-select-all]");
    const selectionBar = root.querySelector("[data-table-selection-bar]");
    const selectionCount = root.querySelector("[data-table-selection-count]");
    const clearSelection = root.querySelector("[data-table-clear-selection]");

    const pageSize = Number.parseInt(root.getAttribute("data-table-page-size") || "0", 10);
    const rows = Array.from(body.rows);

    // A selectable table prepends one cell to every row, so a column's index
    // among the headings is one short of its index among a row's cells.
    const cellOffset = selectAll ? 1 : 0;
    const selected = new Set();

    // The order the server sent is a real answer, not an accident: it is what
    // an unsorted table shows, and what a third click on a sorted column
    // returns to.
    rows.forEach((row, index) => {
        row.dataset.tableIndex = String(index);
    });

    let query = "";
    let sortIndex = -1;
    let sortDirection = "ascending";
    let page = 1;

    function cellValue(row, index) {
        const cell = row.cells[index + cellOffset];
        if (!cell) {
            return "";
        }
        const override = cell.getAttribute("data-sort-value");
        return (override !== null && override !== "" ? override : cell.textContent).trim();
    }

    function compare(a, b, index, type) {
        const left = cellValue(a, index);
        const right = cellValue(b, index);

        if (type === "number") {
            const leftNumber = parseNumber(left);
            const rightNumber = parseNumber(right);
            if (leftNumber === null || rightNumber === null) return 0;
            return leftNumber - rightNumber;
        }

        if (type === "date") {
            const leftDate = parseDate(left);
            const rightDate = parseDate(right);
            if (leftDate === null || rightDate === null) return 0;
            return leftDate - rightDate;
        }

        return left.localeCompare(right, undefined, { numeric: true, sensitivity: "base" });
    }

    // A row with nothing in the sorted column has no place in the ordering,
    // so it goes to the end - and stays there in both directions. Reversing
    // blanks along with the data would park the empty rows at the top of a
    // descending sort, in front of the values the reader asked to see.
    function isBlankFor(row, index, type) {
        const raw = cellValue(row, index);
        if (type === "number") return parseNumber(raw) === null;
        if (type === "date") return parseDate(raw) === null;
        return false;
    }

    function applySort() {
        if (sortIndex < 0) {
            rows.sort((a, b) => Number(a.dataset.tableIndex) - Number(b.dataset.tableIndex));
        } else {
            const type = headings[sortIndex] ? headings[sortIndex].getAttribute("data-table-type") || "text" : "text";
            const direction = sortDirection === "ascending" ? 1 : -1;
            rows.sort((a, b) => {
                const blankA = isBlankFor(a, sortIndex, type);
                const blankB = isBlankFor(b, sortIndex, type);
                if (blankA !== blankB) {
                    return blankA ? 1 : -1;
                }
                const result = compare(a, b, sortIndex, type);
                // Ties keep the server's order rather than drifting between
                // sorts, which is what makes repeated sorting feel stable.
                return result !== 0
                    ? result * direction
                    : Number(a.dataset.tableIndex) - Number(b.dataset.tableIndex);
            });
        }

        rows.forEach((row) => body.appendChild(row));

        headings.forEach((heading, index) => {
            if (!heading.hasAttribute("aria-sort")) {
                return;
            }
            const active = index === sortIndex;
            heading.setAttribute("aria-sort", active ? sortDirection : "none");
            const icon = heading.querySelector("[data-table-sort-icon]");
            if (icon) {
                icon.innerHTML = active ? (sortDirection === "ascending" ? "&uarr;" : "&darr;") : "&#8645;";
                icon.classList.toggle("text-blue-600", active);
                icon.classList.toggle("dark:text-blue-400", active);
                icon.classList.toggle("text-slate-400", !active);
                icon.classList.toggle("dark:text-slate-500", !active);
            }
        });
    }

    function matches(row) {
        return query === "" || row.textContent.toLowerCase().includes(query);
    }

    /** The rows actually on screen right now - render() has just set this. */
    function visibleOnPage() {
        return rows.filter((row) => !row.hidden);
    }

    // Selection is tracked by row key, never by position, so it survives
    // sorting, searching and paging: the rows themselves are only ever
    // reordered or hidden, and the checkbox a user ticked is still the same
    // element afterwards.
    function syncSelection(visibleOnPage) {
        if (!selectAll) {
            return;
        }

        rows.forEach((row) => {
            const key = row.getAttribute("data-row-key");
            const checkbox = row.querySelector("[data-table-select-row]");
            const isSelected = key !== null && selected.has(key);
            if (checkbox) {
                checkbox.checked = isSelected;
            }
            row.classList.toggle("bg-blue-50/60", isSelected);
            row.classList.toggle("dark:bg-blue-950/40", isSelected);
        });

        const selectableOnPage = visibleOnPage.filter((row) => row.getAttribute("data-row-key") !== null);
        const selectedOnPage = selectableOnPage.filter((row) => selected.has(row.getAttribute("data-row-key"))).length;
        selectAll.checked = selectableOnPage.length > 0 && selectedOnPage === selectableOnPage.length;
        selectAll.indeterminate = selectedOnPage > 0 && selectedOnPage < selectableOnPage.length;

        if (selectionBar) {
            const hasSelection = selected.size > 0;
            selectionBar.classList.toggle("hidden", !hasSelection);
            selectionBar.classList.toggle("flex", hasSelection);
            if (hasSelection && selectionCount) {
                selectionCount.textContent = `${selected.size} selected`;
            }
        }
    }

    function render() {
        const visible = rows.filter(matches);
        const totalPages = pageSize > 0 ? Math.max(1, Math.ceil(visible.length / pageSize)) : 1;
        if (page > totalPages) {
            page = totalPages;
        }

        const start = pageSize > 0 ? (page - 1) * pageSize : 0;
        const end = pageSize > 0 ? start + pageSize : visible.length;

        rows.forEach((row) => {
            row.hidden = true;
        });
        const onPage = visible.slice(start, end);
        onPage.forEach((row) => {
            row.hidden = false;
        });
        syncSelection(onPage);

        // An empty result from a search is recoverable and says so; an empty
        // table was rendered empty by the server and is left exactly as it
        // was found.
        const filteredToNothing = visible.length === 0 && rows.length > 0;
        if (emptyFiltered) {
            emptyFiltered.classList.toggle("hidden", !filteredToNothing);
        }
        if (scroll && rows.length > 0) {
            scroll.classList.toggle("hidden", filteredToNothing);
        }
        if (emptyNone && rows.length > 0) {
            emptyNone.classList.add("hidden");
        }

        if (status) {
            if (rows.length === 0) {
                status.textContent = "";
            } else if (visible.length === 0) {
                status.textContent = `No records match “${search ? search.value.trim() : ""}”`;
            } else if (pageSize > 0) {
                status.textContent = `Showing ${start + 1}–${Math.min(end, visible.length)} of ${visible.length} records`;
            } else {
                status.textContent = `Showing ${visible.length} of ${rows.length} records`;
            }
        }

        if (pageIndicator) {
            pageIndicator.textContent = `Page ${page} of ${totalPages}`;
        }
        if (previous) {
            previous.disabled = page <= 1 || visible.length === 0;
        }
        if (next) {
            next.disabled = page >= totalPages || visible.length === 0;
        }
    }

    sortButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const heading = button.closest("[data-table-column]");
            const index = headings.indexOf(heading);
            if (index < 0) {
                return;
            }

            // Ascending, descending, then back to the order the server sent -
            // so a sort can always be undone without reloading the page.
            if (sortIndex !== index) {
                sortIndex = index;
                sortDirection = "ascending";
            } else if (sortDirection === "ascending") {
                sortDirection = "descending";
            } else {
                sortIndex = -1;
                sortDirection = "ascending";
            }

            page = 1;
            applySort();
            render();
        });
    });

    if (search) {
        search.addEventListener("input", () => {
            query = search.value.trim().toLowerCase();
            page = 1;
            render();
        });
    }

    // One delegated listener on the body rather than one per row: the rows
    // are server-rendered and never replaced, so there is nothing to re-bind
    // after sorting or paging, and a table of any size costs one listener.
    if (selectAll) {
        body.addEventListener("change", (event) => {
            const checkbox = event.target.closest("[data-table-select-row]");
            if (!checkbox) {
                return;
            }
            const key = checkbox.closest("[data-table-row]").getAttribute("data-row-key");
            if (key === null) {
                return;
            }
            if (checkbox.checked) {
                selected.add(key);
            } else {
                selected.delete(key);
            }
            render();
        });

        selectAll.addEventListener("change", () => {
            // Scoped to what is on screen, matching the control's own label.
            visibleOnPage().forEach((row) => {
                const key = row.getAttribute("data-row-key");
                if (key === null) {
                    return;
                }
                if (selectAll.checked) {
                    selected.add(key);
                } else {
                    selected.delete(key);
                }
            });
            render();
        });

        if (clearSelection) {
            clearSelection.addEventListener("click", () => {
                selected.clear();
                render();
            });
        }
    }

    if (previous) {
        previous.addEventListener("click", () => {
            if (page > 1) {
                page -= 1;
                render();
            }
        });
    }

    if (next) {
        next.addEventListener("click", () => {
            page += 1;
            render();
        });
    }

    render();
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-table]").forEach(initTable);
});
