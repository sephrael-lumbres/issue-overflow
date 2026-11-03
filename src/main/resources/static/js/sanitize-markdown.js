// Turns the HTML a Markdown renderer produced from user text into HTML that is
// safe to assign to innerHTML. Needs DOMPurify (/js/vendor/purify.min.js).
//
// DOMPurify's defaults remove scripts, event handlers and JavaScript: URLs.
// User Markdown also may not restyle the page or add form controls: a <style>
// tag or style attribute could overlay the page or read the CSRF fields with
// attribute selectors, and a form could post to another site.
function sanitizeMarkdownHtml(html) {
    return DOMPurify.sanitize(html, {
        FORBID_TAGS: ['style', 'form', 'input', 'button', 'textarea', 'select', 'option'],
        FORBID_ATTR: ['style']
    });
}
