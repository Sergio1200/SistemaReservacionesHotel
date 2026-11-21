const menuToggle = document.querySelector("[data-menu-toggle]");

if (menuToggle) {
    const sidebar = menuToggle.closest(".app-sidebar");
    const setMenuOpen = (open) => {
        sidebar.classList.toggle("menu-open", open);
        menuToggle.setAttribute("aria-expanded", String(open));
    };

    document.documentElement.classList.add("menu-con-js");

    menuToggle.addEventListener("click", () => {
        setMenuOpen(menuToggle.getAttribute("aria-expanded") === "false");
    });

    sidebar.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && menuToggle.getAttribute("aria-expanded") === "true") {
            setMenuOpen(false);
            menuToggle.focus();
        }
    });
}

document.querySelectorAll("form[data-prototipo]").forEach((form) => {
    form.addEventListener("submit", (event) => event.preventDefault());
});
