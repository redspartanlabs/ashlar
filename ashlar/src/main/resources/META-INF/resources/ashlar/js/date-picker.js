const MONTH_NAMES_LONG = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
];
const MONTH_NAMES_SHORT = [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
];

const YEAR_PAGE_SIZE = 9;

function pad(value) {
    return String(value).padStart(2, "0");
}

function toIsoDate(date) {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

// Parsed manually (rather than `new Date(isoString)`) because the built-in
// ISO parser treats the string as UTC midnight, which can shift to the
// previous day once read back with local getters in negative-UTC timezones.
function parseIsoDate(value) {
    if (!value) {
        return null;
    }
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (!match) {
        return null;
    }
    const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
    date.setHours(0, 0, 0, 0);
    return date;
}

function isSameDay(a, b) {
    return a.getFullYear() === b.getFullYear()
        && a.getMonth() === b.getMonth()
        && a.getDate() === b.getDate();
}

function describeDate(date) {
    return date.toLocaleDateString(undefined, { year: "numeric", month: "long", day: "numeric" });
}

// The small, fixed counterpart to DateDisplayFormat.java. Java formats the
// initial server-rendered value; this formats every value after that, once
// the calendar is running client-side.
function formatDate(date, pattern) {
    const year = date.getFullYear();
    const month = date.getMonth();
    const day = date.getDate();

    switch (pattern) {
        case "MM/dd/yyyy":
            return `${pad(month + 1)}/${pad(day)}/${year}`;
        case "dd/MM/yyyy":
            return `${pad(day)}/${pad(month + 1)}/${year}`;
        case "yyyy-MM-dd":
            return toIsoDate(date);
        case "MMMM d, yyyy":
            return `${MONTH_NAMES_LONG[month]} ${day}, ${year}`;
        case "MMM d, yyyy":
        default:
            return `${MONTH_NAMES_SHORT[month]} ${day}, ${year}`;
    }
}

// Applies the same visual/roving-tabindex/disabled treatment to a day, month,
// or year cell button, so the three grids read as one consistent component
// rather than three unrelated screens.
function styleGridButton(button, { isSelected, isCurrent, isFocusable, isDisabled }) {
    button.tabIndex = isFocusable ? 0 : -1;
    button.disabled = isDisabled;

    if (isSelected) {
        button.className = "rounded-md p-2 bg-blue-600 text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500";
    } else if (isCurrent) {
        button.className = "rounded-md p-2 font-semibold text-slate-900 ring-1 ring-sky-400 hover:bg-sky-50 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-slate-100 dark:ring-sky-500 dark:hover:bg-slate-700";
    } else if (isDisabled) {
        button.className = "rounded-md p-2 text-slate-300 cursor-not-allowed dark:text-slate-600";
    } else {
        button.className = "rounded-md p-2 text-slate-700 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-slate-300 dark:hover:bg-slate-700";
    }
}

function initDatePicker(root) {
    const trigger = root.querySelector("[data-date-picker-trigger]");
    const valueLabel = root.querySelector("[data-date-picker-value]");
    const valueInput = root.querySelector("[data-date-picker-input]");
    const clearButton = root.querySelector("[data-date-picker-clear]");
    const panel = root.querySelector("[data-date-picker-panel]");
    const title = root.querySelector("[data-date-picker-title]");
    const weekdaysRow = root.querySelector("[data-date-picker-weekdays]");
    const grid = root.querySelector("[data-date-picker-days]");
    const prevButton = root.querySelector("[data-date-picker-prev]");
    const nextButton = root.querySelector("[data-date-picker-next]");
    const todayButton = root.querySelector("[data-date-picker-today]");
    const status = root.querySelector("[data-date-picker-status]");
    const errorMessage = root.querySelector("[data-date-picker-error]");

    // Captured once, before any state changes: the trigger's own
    // server-rendered classes are the "valid" look, and its
    // data-date-picker-error-classes attribute (computed by the same
    // ButtonStyles-style pattern used elsewhere in this project) is the
    // "invalid" look. Swapping between two known class sets - rather than
    // hardcoding color names here - is what keeps this from drifting out of
    // sync with the template, the same fix already applied to the toggle
    // button component.
    const normalTriggerClasses = trigger.className.split(/\s+/).filter(Boolean);
    const errorTriggerClasses = (trigger.dataset.errorClasses || "").split(/\s+/).filter(Boolean);

    const format = root.dataset.format || "MMM d, yyyy";
    const placeholder = root.dataset.placeholder || "Select a date";
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // The one date-range rule the whole component defers to. Day, Month, and
    // Year views all build on this instead of inventing their own rules.
    //
    // min/max are re-read from the dataset on every call rather than cached
    // once at init, so an instance's constraints can be updated live after
    // the fact - date-range-picker.js relies on exactly this to keep its
    // start/end pickers from ever allowing an out-of-order selection,
    // without reaching into this closure or reimplementing calendar logic.
    function isDisabledDate(date) {
        const minDate = parseIsoDate(root.dataset.minDate);
        const maxDate = parseIsoDate(root.dataset.maxDate);
        if (minDate && date < minDate) {
            return true;
        }
        return maxDate !== null && date > maxDate;
    }

    function hasEnabledDayInMonth(year, month) {
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        for (let day = 1; day <= daysInMonth; day++) {
            if (!isDisabledDate(new Date(year, month, day))) {
                return true;
            }
        }
        return false;
    }

    function hasEnabledDayInYear(year) {
        for (let month = 0; month < 12; month++) {
            if (hasEnabledDayInMonth(year, month)) {
                return true;
            }
        }
        return false;
    }

    function firstEnabledDayOrFirstOfMonth(year, month) {
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        for (let day = 1; day <= daysInMonth; day++) {
            const candidate = new Date(year, month, day);
            if (!isDisabledDate(candidate)) {
                return candidate;
            }
        }
        return new Date(year, month, 1);
    }

    let selectedDate = parseIsoDate(valueInput.value);
    let viewYear = (selectedDate || today).getFullYear();
    let viewMonth = (selectedDate || today).getMonth();
    let focusedDate = selectedDate || (isDisabledDate(today) ? firstEnabledDayOrFirstOfMonth(viewYear, viewMonth) : today);

    // "view" is the only new piece of top-level state this feature adds.
    // Month/Year views get their own small roving-focus trackers, the same
    // pattern focusedDate already uses for Day view.
    let view = "DAY"; // "DAY" | "MONTH" | "YEAR"
    let focusedMonth = viewMonth;
    let focusedYear = viewYear;
    let yearRangeStart = viewYear - 4;

    function announce(message) {
        if (status) {
            status.textContent = message;
        }
    }

    // Reflects an actual validation failure (a real submit attempt while
    // required and empty) - never set just because the field was focused,
    // opened, or tabbed away from.
    function showError() {
        trigger.classList.remove(...normalTriggerClasses);
        trigger.classList.add(...errorTriggerClasses);
        trigger.setAttribute("aria-invalid", "true");
        if (errorMessage) {
            trigger.setAttribute("aria-describedby", errorMessage.id);
            errorMessage.classList.remove("hidden");
        }
    }

    function clearError() {
        trigger.classList.remove(...errorTriggerClasses);
        trigger.classList.add(...normalTriggerClasses);
        trigger.removeAttribute("aria-invalid");
        if (errorMessage) {
            trigger.removeAttribute("aria-describedby");
            errorMessage.classList.add("hidden");
        }
    }

    function updateTriggerLabel() {
        if (selectedDate) {
            valueLabel.textContent = formatDate(selectedDate, format);
            valueLabel.classList.remove("text-slate-400");
        } else {
            valueLabel.textContent = placeholder;
            valueLabel.classList.add("text-slate-400");
        }

        if (clearButton) {
            clearButton.classList.toggle("hidden", !selectedDate);
            clearButton.classList.toggle("flex", Boolean(selectedDate));
        }
    }

    function renderHeader() {
        if (view === "DAY") {
            const label = new Date(viewYear, viewMonth, 1).toLocaleDateString(undefined, { month: "long", year: "numeric" });
            title.textContent = label;
            title.disabled = false;
            title.setAttribute("aria-label", `${label}, choose month`);
            prevButton.setAttribute("aria-label", "Previous month");
            nextButton.setAttribute("aria-label", "Next month");
        } else if (view === "MONTH") {
            title.textContent = String(viewYear);
            title.disabled = false;
            title.setAttribute("aria-label", `${viewYear}, choose year`);
            prevButton.setAttribute("aria-label", "Previous year");
            nextButton.setAttribute("aria-label", "Next year");
        } else {
            title.textContent = `${yearRangeStart}–${yearRangeStart + YEAR_PAGE_SIZE - 1}`;
            title.disabled = true;
            title.removeAttribute("aria-label");
            prevButton.setAttribute("aria-label", "Previous years");
            nextButton.setAttribute("aria-label", "Next years");
        }
    }

    function createDayButton(cellDate) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = String(cellDate.getDate());

        let accessibleLabel = describeDate(cellDate);
        if (isSameDay(cellDate, today)) {
            accessibleLabel += " (Today)";
        }
        if (selectedDate !== null && isSameDay(cellDate, selectedDate)) {
            accessibleLabel += " (Selected)";
        }
        button.setAttribute("aria-label", accessibleLabel);

        styleGridButton(button, {
            isSelected: selectedDate !== null && isSameDay(cellDate, selectedDate),
            isCurrent: isSameDay(cellDate, today),
            isFocusable: isSameDay(cellDate, focusedDate),
            isDisabled: isDisabledDate(cellDate),
        });

        button.addEventListener("click", () => selectDate(cellDate));
        return button;
    }

    function renderDayGrid() {
        grid.innerHTML = "";

        const firstOfMonth = new Date(viewYear, viewMonth, 1);
        const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
        const leadingBlanks = firstOfMonth.getDay();

        for (let i = 0; i < leadingBlanks; i++) {
            grid.appendChild(document.createElement("div"));
        }

        for (let day = 1; day <= daysInMonth; day++) {
            grid.appendChild(createDayButton(new Date(viewYear, viewMonth, day)));
        }
    }

    function createMonthButton(month) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = MONTH_NAMES_SHORT[month];
        button.setAttribute("aria-label", `${MONTH_NAMES_LONG[month]} ${viewYear}`);

        styleGridButton(button, {
            isSelected: selectedDate !== null && selectedDate.getFullYear() === viewYear && selectedDate.getMonth() === month,
            isCurrent: today.getFullYear() === viewYear && today.getMonth() === month,
            isFocusable: focusedMonth === month,
            isDisabled: !hasEnabledDayInMonth(viewYear, month),
        });

        button.addEventListener("click", () => selectMonth(month));
        return button;
    }

    function renderMonthGrid() {
        grid.innerHTML = "";
        for (let month = 0; month < 12; month++) {
            grid.appendChild(createMonthButton(month));
        }
    }

    function createYearButton(year) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = String(year);
        button.setAttribute("aria-label", String(year));

        styleGridButton(button, {
            isSelected: selectedDate !== null && selectedDate.getFullYear() === year,
            isCurrent: today.getFullYear() === year,
            isFocusable: focusedYear === year,
            isDisabled: !hasEnabledDayInYear(year),
        });

        button.addEventListener("click", () => selectYear(year));
        return button;
    }

    function renderYearGrid() {
        grid.innerHTML = "";
        for (let year = yearRangeStart; year < yearRangeStart + YEAR_PAGE_SIZE; year++) {
            grid.appendChild(createYearButton(year));
        }
    }

    function renderGrid() {
        if (view === "DAY") {
            weekdaysRow.classList.remove("hidden");
            grid.className = "grid grid-cols-7 gap-1 text-center";
            renderDayGrid();
        } else if (view === "MONTH") {
            weekdaysRow.classList.add("hidden");
            grid.className = "grid grid-cols-3 gap-2 text-center";
            renderMonthGrid();
        } else {
            weekdaysRow.classList.add("hidden");
            grid.className = "grid grid-cols-3 gap-2 text-center";
            renderYearGrid();
        }
    }

    function updateTodayButton() {
        if (todayButton) {
            todayButton.disabled = isDisabledDate(today);
        }
    }

    function render() {
        renderHeader();
        renderGrid();
        updateTodayButton();
    }

    function focusRovingButton() {
        const current = grid.querySelector('button[tabindex="0"]');
        if (current) {
            current.focus();
        }
    }

    function isOpen() {
        return !panel.classList.contains("hidden");
    }

    function openPanel() {
        view = "DAY"; // always reopen at the primary view
        render();
        panel.classList.remove("hidden");
        trigger.setAttribute("aria-expanded", "true");
        focusRovingButton();
    }

    function closePanel(returnFocus) {
        panel.classList.add("hidden");
        trigger.setAttribute("aria-expanded", "false");
        if (returnFocus) {
            trigger.focus();
        }
    }

    function selectDate(date) {
        if (isDisabledDate(date)) {
            return;
        }
        selectedDate = date;
        focusedDate = date;
        valueInput.value = toIsoDate(date);
        // Setting .value programmatically never fires a native 'change' on
        // its own - dispatched here (matching select.js/combobox.js's own
        // hidden controls) so external code, like date-range-picker.js,
        // can react to a selection without reaching into this closure.
        valueInput.dispatchEvent(new Event("change", { bubbles: true }));
        updateTriggerLabel();
        clearError();
        announce(`Selected ${describeDate(date)}`);
        closePanel(true);
    }

    function clearSelection() {
        selectedDate = null;
        valueInput.value = "";
        valueInput.dispatchEvent(new Event("change", { bubbles: true }));
        updateTriggerLabel();
        announce("Date cleared");
        trigger.focus();
    }

    function changeMonth(offset) {
        viewMonth += offset;
        if (viewMonth < 0) {
            viewMonth = 11;
            viewYear -= 1;
        } else if (viewMonth > 11) {
            viewMonth = 0;
            viewYear += 1;
        }
        focusedDate = firstEnabledDayOrFirstOfMonth(viewYear, viewMonth);
        render();
        focusRovingButton();
    }

    function changeYear(offset) {
        viewYear += offset;
        render();
        focusRovingButton();
    }

    function changeYearRange(offset) {
        yearRangeStart += offset;
        focusedYear = Math.min(Math.max(focusedYear, yearRangeStart), yearRangeStart + YEAR_PAGE_SIZE - 1);
        render();
        focusRovingButton();
    }

    function goToPrevious() {
        if (view === "DAY") {
            changeMonth(-1);
        } else if (view === "MONTH") {
            changeYear(-1);
        } else {
            changeYearRange(-YEAR_PAGE_SIZE);
        }
    }

    function goToNext() {
        if (view === "DAY") {
            changeMonth(1);
        } else if (view === "MONTH") {
            changeYear(1);
        } else {
            changeYearRange(YEAR_PAGE_SIZE);
        }
    }

    function openMonthView() {
        view = "MONTH";
        focusedMonth = viewMonth;
        render();
        focusRovingButton();
    }

    function openYearView() {
        view = "YEAR";
        yearRangeStart = viewYear - 4;
        focusedYear = viewYear;
        render();
        focusRovingButton();
    }

    function selectMonth(month) {
        if (!hasEnabledDayInMonth(viewYear, month)) {
            return;
        }
        viewMonth = month;
        view = "DAY";
        focusedDate = (selectedDate && selectedDate.getFullYear() === viewYear && selectedDate.getMonth() === viewMonth)
            ? selectedDate
            : firstEnabledDayOrFirstOfMonth(viewYear, viewMonth);
        render();
        focusRovingButton();
    }

    function selectYear(year) {
        if (!hasEnabledDayInYear(year)) {
            return;
        }
        viewYear = year;
        view = "MONTH";
        focusedMonth = viewMonth;
        render();
        focusRovingButton();
    }

    function moveFocusByDays(offset) {
        const next = new Date(focusedDate);
        next.setDate(next.getDate() + offset);
        if (isDisabledDate(next)) {
            return;
        }
        focusedDate = next;
        if (next.getMonth() !== viewMonth || next.getFullYear() !== viewYear) {
            viewYear = next.getFullYear();
            viewMonth = next.getMonth();
        }
        renderDayGrid();
        focusRovingButton();
    }

    function moveFocusTo(date) {
        if (isDisabledDate(date)) {
            return;
        }
        focusedDate = date;
        renderDayGrid();
        focusRovingButton();
    }

    // Arrow keys within Month/Year views clamp at the visible grid's edges
    // (Month) or page to the next/previous year window (Year), the same way
    // moveFocusByDays crosses a month boundary in Day view. Landing on a
    // disabled cell is refused outright, since a disabled button can't
    // actually receive focus - the same tradeoff already made for days.
    function moveMonthFocusBy(offset) {
        const next = focusedMonth + offset;
        if (next < 0 || next > 11 || !hasEnabledDayInMonth(viewYear, next)) {
            return;
        }
        focusedMonth = next;
        renderMonthGrid();
        focusRovingButton();
    }

    function moveYearFocusBy(offset) {
        const next = focusedYear + offset;
        if (!hasEnabledDayInYear(next)) {
            return;
        }
        if (next < yearRangeStart) {
            yearRangeStart -= YEAR_PAGE_SIZE;
        } else if (next > yearRangeStart + YEAR_PAGE_SIZE - 1) {
            yearRangeStart += YEAR_PAGE_SIZE;
        }
        focusedYear = next;
        renderYearGrid();
        focusRovingButton();
    }

    trigger.addEventListener("click", (event) => {
        event.stopPropagation();
        isOpen() ? closePanel(false) : openPanel();
    });

    trigger.addEventListener("keydown", (event) => {
        if (event.key === "ArrowDown" && !isOpen()) {
            event.preventDefault();
            openPanel();
        }
    });

    if (clearButton) {
        clearButton.addEventListener("click", (event) => {
            event.stopPropagation();
            clearSelection();
        });
    }

    // Fires automatically, natively, when an enclosing <form> is submitted
    // while this required field is empty - no submit listener of our own is
    // needed. preventDefault() only suppresses the browser's own bubble/
    // focus for this field (which would otherwise point at an invisible
    // input); it does not mark the field valid or allow the form through.
    valueInput.addEventListener("invalid", (event) => {
        event.preventDefault();
        showError();
    });

    title.addEventListener("click", () => {
        if (view === "DAY") {
            openMonthView();
        } else if (view === "MONTH") {
            openYearView();
        }
    });

    prevButton.addEventListener("click", goToPrevious);
    nextButton.addEventListener("click", goToNext);

    if (todayButton) {
        todayButton.addEventListener("click", () => {
            view = "DAY";
            viewYear = today.getFullYear();
            viewMonth = today.getMonth();
            selectDate(today);
        });
    }

    grid.addEventListener("keydown", (event) => {
        if (view === "DAY") {
            handleDayGridKeydown(event);
        } else if (view === "MONTH") {
            handleMonthGridKeydown(event);
        } else {
            handleYearGridKeydown(event);
        }
    });

    function handleDayGridKeydown(event) {
        switch (event.key) {
            case "ArrowLeft":
                event.preventDefault();
                moveFocusByDays(-1);
                break;
            case "ArrowRight":
                event.preventDefault();
                moveFocusByDays(1);
                break;
            case "ArrowUp":
                event.preventDefault();
                moveFocusByDays(-7);
                break;
            case "ArrowDown":
                event.preventDefault();
                moveFocusByDays(7);
                break;
            case "Home":
                event.preventDefault();
                moveFocusTo(new Date(viewYear, viewMonth, 1));
                break;
            case "End":
                event.preventDefault();
                moveFocusTo(new Date(viewYear, viewMonth + 1, 0));
                break;
            default:
                break;
        }
    }

    function handleMonthGridKeydown(event) {
        switch (event.key) {
            case "ArrowLeft":
                event.preventDefault();
                moveMonthFocusBy(-1);
                break;
            case "ArrowRight":
                event.preventDefault();
                moveMonthFocusBy(1);
                break;
            case "ArrowUp":
                event.preventDefault();
                moveMonthFocusBy(-3);
                break;
            case "ArrowDown":
                event.preventDefault();
                moveMonthFocusBy(3);
                break;
            default:
                break;
        }
    }

    function handleYearGridKeydown(event) {
        switch (event.key) {
            case "ArrowLeft":
                event.preventDefault();
                moveYearFocusBy(-1);
                break;
            case "ArrowRight":
                event.preventDefault();
                moveYearFocusBy(1);
                break;
            case "ArrowUp":
                event.preventDefault();
                moveYearFocusBy(-3);
                break;
            case "ArrowDown":
                event.preventDefault();
                moveYearFocusBy(3);
                break;
            default:
                break;
        }
    }

    root.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && isOpen()) {
            event.preventDefault();
            closePanel(true);
        }
    });

    // Selecting a month/year re-renders the grid (removing the clicked cell)
    // while deliberately keeping the panel open, which detaches
    // event.target from the document before this listener runs.
    // composedPath() is captured at dispatch time, so it still correctly
    // reflects that the click originated inside the component even after
    // that cell has since been replaced.
    document.addEventListener("click", (event) => {
        if (isOpen() && !event.composedPath().includes(root)) {
            closePanel(false);
        }
    });

    // Deferred to a fresh task: removing the previously-focused grid cell
    // during a Day/Month/Year transition fires a synchronous focusout with
    // relatedTarget still null, before this function's own
    // focusRovingButton() call (later in the same transition) moves focus
    // into the new grid. Checking document.activeElement after the current
    // task finishes reflects where focus actually ends up, rather than that
    // transient null.
    root.addEventListener("focusout", () => {
        window.setTimeout(() => {
            if (isOpen() && !root.contains(document.activeElement)) {
                closePanel(false);
            }
        }, 0);
    });

    updateTriggerLabel();
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-date-picker]").forEach(initDatePicker);
});
