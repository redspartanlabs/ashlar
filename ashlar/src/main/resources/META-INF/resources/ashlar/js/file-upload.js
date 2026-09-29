// The only thing native HTML/CSS can't do for this component: reflecting
// which file(s) got picked back into the card's own text. Selecting,
// replacing, and native form submission are otherwise entirely native -
// this never touches input.files itself, only reads it.
function describeSelection(files, multiple) {
    if (files.length === 0) {
        return null;
    }
    if (files.length === 1) {
        return files[0].name;
    }
    // A count rather than every filename - readable regardless of how many
    // files were picked, with no risk of overflowing the card.
    return multiple ? `${files.length} files selected` : files[0].name;
}

function initFileUpload(root) {
    const input = root.querySelector("[data-file-upload-input]");
    const status = root.querySelector("[data-file-upload-status]");
    const idleText = status.dataset.idleText;

    input.addEventListener("change", () => {
        const summary = describeSelection(input.files, input.multiple);
        status.textContent = summary || idleText;
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-file-upload]").forEach(initFileUpload);
});
