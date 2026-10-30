/**
 * Asks before submitting a form whose submit button has a data-confirm message.
 *
 * The message is a plain data attribute, so names and file names in it are never run as a script (an inline onclick
 * would decode them first). Listening in the capture phase also keeps the click from reaching clickable table
 * rows, which would otherwise open the issue while the form submits.
 */
document.addEventListener('click', function (event) {
    const button = event.target.closest('button[data-confirm]');
    if (!button) {
        return;
    }
    event.stopPropagation();
    if (!window.confirm(button.dataset.confirm)) {
        event.preventDefault();
    }
}, true);
