// The preview renders Markdown someone else may have written (an issue description on the edit page),
// so its HTML goes through the issue page's sanitizer (sanitize-markdown.js).
// GFM task-list checkboxes are form controls, so the preview shows those items without a checkbox.
const sanitizedPreview = {
    sanitizerFunction: (renderedHTML) => sanitizeMarkdownHtml(renderedHTML)
};

var easyMDE = new EasyMDE({
    element: document.getElementById('markdownDescriptionEditor'),
    toolbar: ['bold', 'italic', 'strikethrough', 'heading', '|', 'quote', 'code' ,'unordered-list', 'ordered-list', '|', 'link', 'image', '|', 'preview', 'guide'],
    renderingConfig: sanitizedPreview
});

var addComment = new EasyMDE({
    element: document.getElementById('markdownCommentEditor'),
    toolbar: ['bold', 'italic', 'strikethrough', 'heading', '|', 'quote', 'code' ,'unordered-list', 'ordered-list', '|', 'link', 'image', '|', 'preview', 'guide'],
    minHeight: "150px",
    renderingConfig: sanitizedPreview
});

// this allows creation of a Markdown Editor for each comment in an Issue
for(let i=0; i < document.getElementsByClassName("comments").length; i++) {
    var editComment = new EasyMDE({
        element: document.getElementsByClassName('comments').item(i),
        toolbar: ['bold', 'italic', 'strikethrough', 'heading', '|', 'quote', 'code' ,'unordered-list', 'ordered-list', '|', 'link', 'image', '|', 'preview', 'guide'],
        minHeight: "150px",
        renderingConfig: sanitizedPreview
    });
}