document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-drawer-open]").forEach((button) => {
        button.addEventListener("click", () => {
            const drawer = document.querySelector(button.dataset.drawerOpen);
            if (drawer) {
                drawer.classList.add("open");
            }
        });
    });

    document.querySelectorAll("[data-drawer-close]").forEach((button) => {
        button.addEventListener("click", () => {
            const drawer = button.closest(".drawer");
            if (drawer) {
                drawer.classList.remove("open");
            }
        });
    });

    document.querySelectorAll("[data-password-toggle]").forEach((button) => {
        button.addEventListener("click", () => {
            const target = document.querySelector(button.dataset.passwordToggle);
            if (!target) {
                return;
            }
            target.type = target.type === "password" ? "text" : "password";
            button.textContent = target.type === "password" ? "显示" : "隐藏";
        });
    });
});
