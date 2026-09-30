// Parsed manually rather than `new Date(isoString)` for the same reason
// date-picker.js does: the built-in ISO parser treats the string as UTC
// midnight, which can shift a day in negative-UTC timezones once read back
// with local getters.
function parseIsoDate(value) {
    if (!value) {
        return null;
    }
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (!match) {
        return null;
    }
    return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
}

function initDateRangePicker(root) {
    const startRoot = root.querySelector("[data-date-range-start] [data-date-picker]");
    const endRoot = root.querySelector("[data-date-range-end] [data-date-picker]");
    const startInput = startRoot.querySelector("[data-date-picker-input]");
    const endInput = endRoot.querySelector("[data-date-picker-input]");
    const errorMessage = root.querySelector("[data-date-range-error]");

    // Keeps each picker's own `min`/`max` in sync with the other's current
    // value. Because date-picker.js re-reads these live (rather than once
    // at init), this alone stops an out-of-order date from ever being
    // selectable through either calendar - the dates just render disabled,
    // the same way an application-supplied min/max already would.
    function syncConstraints() {
        if (startInput.value) {
            endRoot.dataset.minDate = startInput.value;
        } else {
            delete endRoot.dataset.minDate;
        }
        if (endInput.value) {
            startRoot.dataset.maxDate = endInput.value;
        } else {
            delete startRoot.dataset.maxDate;
        }
    }

    // Defense in depth for a range that arrived already invalid (e.g. bad
    // initial server data) rather than one assembled through the calendar,
    // where syncConstraints() above already prevents the problem. This
    // never touches either Date Picker's own required-field error state -
    // a range-order problem isn't "no date was chosen", and reusing that
    // display would show its unrelated default message.
    function isRangeInvalid() {
        const start = parseIsoDate(startInput.value);
        const end = parseIsoDate(endInput.value);
        return Boolean(start && end && end < start);
    }

    function updateRangeError(invalid) {
        if (errorMessage) {
            errorMessage.classList.toggle("hidden", !invalid);
        }
    }

    function handleChange() {
        syncConstraints();
        updateRangeError(isRangeInvalid());
    }

    startInput.addEventListener("change", handleChange);
    endInput.addEventListener("change", handleChange);

    const form = root.closest("form");
    if (form) {
        form.addEventListener("submit", (event) => {
            const invalid = isRangeInvalid();
            updateRangeError(invalid);
            if (invalid) {
                event.preventDefault();
                endRoot.querySelector("[data-date-picker-trigger]").focus();
            }
        });
    }

    handleChange();
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-date-range-picker]").forEach(initDateRangePicker);
});
