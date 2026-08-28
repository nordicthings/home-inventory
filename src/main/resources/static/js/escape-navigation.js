(function () {
    document.addEventListener("keydown", function (event) {
        if (event.key !== "Escape" || event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) {
            return;
        }

        var target = document.querySelector("[data-escape-href]");
        if (!target) {
            return;
        }

        var href = target.getAttribute("data-escape-href");
        if (!href) {
            return;
        }

        event.preventDefault();
        window.location.assign(href);
    });
})();
