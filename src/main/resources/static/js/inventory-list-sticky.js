(function () {
    function updateInventoryStickyOffset() {
        var page = document.querySelector(".page--inventory");
        if (!page) {
            return;
        }

        var topbar = document.querySelector(".topbar");
        var header = page.querySelector(".page__header");
        var toolbar = page.querySelector(".toolbar");
        var topbarHeight = topbar ? topbar.offsetHeight : 0;
        var headerHeight = header ? header.offsetHeight : 0;
        var toolbarHeight = toolbar ? toolbar.offsetHeight : 0;
        var offset = topbarHeight + headerHeight + toolbarHeight;

        document.documentElement.style.setProperty("--inventory-topbar-height", topbarHeight + "px");
        document.documentElement.style.setProperty("--inventory-header-height", headerHeight + "px");
        document.documentElement.style.setProperty("--inventory-sticky-offset", offset + "px");
    }

    window.addEventListener("load", updateInventoryStickyOffset);
    window.addEventListener("resize", updateInventoryStickyOffset);
    document.addEventListener("htmx:afterSwap", updateInventoryStickyOffset);
    updateInventoryStickyOffset();
})();
