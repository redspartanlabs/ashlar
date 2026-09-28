// Formats/normalizes the small set of built-in masks a text-input can opt
// into via `mask="..."` (text-input.jte). This is deliberately NOT a
// masking framework - each entry is a hand-written formatter for one
// real-world input shape, not a declarative pattern language. Add a new
// mask by adding one entry here; nothing else in this file changes.
const MASKS = {
    phone: {
        // The canonical digits behind a phone value, and how many leading
        // raw digits were dropped to get there (currently just a US
        // country code on an 11-digit paste, e.g. from a contacts app) -
        // the offset lets the caret math below account for characters
        // that disappeared from the *front* of the value, not just the
        // truncation at the end.
        canonicalDigits(rawValue) {
            let digits = digitsOnly(rawValue);
            let offset = 0;
            if (digits.length === 11 && digits.startsWith("1")) {
                digits = digits.slice(1);
                offset = 1;
            }
            return { digits: digits.slice(0, 10), offset };
        },
        // Formats up to 10 digits as a US phone number, partially as the
        // user types: "5" -> "(5", "5055" -> "(505) 5",
        // "5055551234" -> "(505) 555-1234".
        format(digits) {
            if (digits.length === 0) {
                return "";
            }
            if (digits.length < 4) {
                return `(${digits}`;
            }
            if (digits.length < 7) {
                return `(${digits.slice(0, 3)}) ${digits.slice(3)}`;
            }
            return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`;
        },
    },
};

function digitsOnly(value) {
    return value.replace(/\D/g, "");
}

function initMaskedInput(input) {
    const mask = MASKS[input.dataset.mask];
    if (!mask) {
        return;
    }

    function reformat() {
        // Digits before the caret in the *current* value - the browser has
        // already applied the user's edit (an inserted character, a
        // deleted range, a paste) by the time this fires. Counting digits
        // rather than raw characters, and correcting for any digits the
        // canonical form drops from the front, is what lets the caret land
        // back in the same logical spot after reformatting, across typing,
        // deleting, and pasting alike.
        const caret = input.selectionStart;
        const rawDigitsBeforeCaret = digitsOnly(input.value.slice(0, caret)).length;

        const { digits, offset } = mask.canonicalDigits(input.value);
        const digitsBeforeCaret = Math.max(0, rawDigitsBeforeCaret - offset);

        const formatted = mask.format(digits);
        input.value = formatted;

        let newCaret = formatted.length;
        if (digitsBeforeCaret === 0) {
            newCaret = 0;
        } else {
            let seen = 0;
            for (let i = 0; i < formatted.length; i++) {
                if (/\d/.test(formatted[i])) {
                    seen++;
                    if (seen === digitsBeforeCaret) {
                        newCaret = i + 1;
                        break;
                    }
                }
            }
        }

        // Selection range isn't supported for every input type (e.g.
        // "number") - harmless to skip if the browser refuses it.
        try {
            input.setSelectionRange(newCaret, newCaret);
        } catch (error) {
            // Not selectable - the value is still correctly formatted.
        }
    }

    input.addEventListener("input", reformat);

    // Submits the canonical digits ("5055551234"), not the formatted
    // display value ("(505) 555-1234"). This only runs once the browser
    // has already run constraint validation and decided the form is
    // valid - the visible field is about to navigate away with it, so
    // there's no moment where the user sees the stripped-down value.
    if (input.form) {
        input.form.addEventListener("submit", () => {
            input.value = mask.canonicalDigits(input.value).digits;
        });
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-mask]").forEach(initMaskedInput);
});
