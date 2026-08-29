(function () {
    var scrollKey = "home-inventory.items.scrollTop";

    function isInventoryListPage() {
        return document.querySelector(".page--inventory") !== null;
    }

    function scrollContainer() {
        return document.querySelector("[data-inventory-scroll-container]");
    }

    function saveScrollPosition(event) {
        var link = event.target.closest("[data-restore-list-scroll]");
        var container = scrollContainer();
        if (!link || !isInventoryListPage() || !container) {
            return;
        }

        try {
            sessionStorage.setItem(scrollKey, String(container.scrollTop || 0));
        } catch (error) {
            return;
        }
    }

    function restoreScrollPosition() {
        if (!isInventoryListPage()) {
            return;
        }

        var container = scrollContainer();
        if (!container) {
            return;
        }

        var storedPosition;

        try {
            storedPosition = sessionStorage.getItem(scrollKey);
        } catch (error) {
            return;
        }

        if (storedPosition === null) {
            return;
        }

        try {
            sessionStorage.removeItem(scrollKey);
        } catch (error) {
            return;
        }

        window.requestAnimationFrame(function () {
            container.scrollTop = Number(storedPosition) || 0;
        });
    }

    document.addEventListener("click", saveScrollPosition);
    window.addEventListener("load", restoreScrollPosition);
})();
