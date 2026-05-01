(function () {
  const storageKey = "dbflow-admin-theme";
  const allowedModes = new Set(["system", "light", "dark"]);
  const media = window.matchMedia
    ? window.matchMedia("(prefers-color-scheme: dark)")
    : null;

  function storedMode() {
    try {
      const value = window.localStorage.getItem(storageKey);
      return allowedModes.has(value) ? value : "system";
    } catch (error) {
      return "system";
    }
  }

  function effectiveTheme(mode) {
    if (mode === "dark" || mode === "light") {
      return mode;
    }
    return media && media.matches ? "dark" : "light";
  }

  function applyTheme(mode) {
    const safeMode = allowedModes.has(mode) ? mode : "system";
    const theme = effectiveTheme(safeMode);
    document.documentElement.dataset.theme = theme;
    document.documentElement.dataset.themeMode = safeMode;
    document.querySelectorAll("[data-theme-choice]").forEach((button) => {
      const active = button.dataset.themeChoice === safeMode;
      button.classList.toggle("active", active);
      button.setAttribute("aria-pressed", active ? "true" : "false");
    });
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.setAttribute("aria-pressed", theme === "dark" ? "true" : "false");
      button.setAttribute(
        "aria-label",
        theme === "dark" ? "切换到亮色主题" : "切换到暗色主题",
      );
    });
  }

  function saveTheme(mode) {
    const safeMode = allowedModes.has(mode) ? mode : "system";
    try {
      window.localStorage.setItem(storageKey, safeMode);
    } catch (error) {
      // localStorage 可能被浏览器策略禁用；主题切换仍在当前页面生效。
    }
    applyTheme(safeMode);
  }

  applyTheme(storedMode());

  if (media) {
    media.addEventListener("change", () => {
      if (storedMode() === "system") {
        applyTheme("system");
      }
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    applyTheme(storedMode());

    document.querySelectorAll("[data-theme-choice]").forEach((button) => {
      button.addEventListener("click", () =>
        saveTheme(button.dataset.themeChoice),
      );
    });

    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.addEventListener("click", () => {
        const currentTheme =
          document.documentElement.dataset.theme === "dark" ? "dark" : "light";
        saveTheme(currentTheme === "dark" ? "light" : "dark");
      });
    });

    // 侧边栏底部用户弹出菜单
    const sideUserBtn = document.getElementById("sideUserBtn");
    const sideUserSection = document.getElementById("sideUserSection");
    const sideUserPopup = document.getElementById("sideUserPopup");

    if (sideUserBtn && sideUserSection && sideUserPopup) {
      sideUserBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        const isOpen = sideUserSection.classList.toggle("open");
        sideUserBtn.setAttribute("aria-expanded", isOpen ? "true" : "false");
        sideUserPopup.setAttribute("aria-hidden", isOpen ? "false" : "true");
      });

      document.addEventListener("click", (e) => {
        if (!sideUserSection.contains(e.target)) {
          sideUserSection.classList.remove("open");
          sideUserBtn.setAttribute("aria-expanded", "false");
          sideUserPopup.setAttribute("aria-hidden", "true");
        }
      });

      document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && sideUserSection.classList.contains("open")) {
          sideUserSection.classList.remove("open");
          sideUserBtn.setAttribute("aria-expanded", "false");
          sideUserPopup.setAttribute("aria-hidden", "true");
          sideUserBtn.focus();
        }
      });
    }

    document.querySelectorAll("[data-drawer-open]").forEach((button) => {
      button.addEventListener("click", () => {
        const drawer = document.querySelector(button.dataset.drawerOpen);
        if (drawer) {
          openDrawer(drawer);
          const closeButton = drawer.querySelector("[data-drawer-close]");
          if (closeButton) {
            closeButton.focus();
          }
        }
      });
    });

    document.querySelectorAll("[data-drawer-close]").forEach((button) => {
      button.addEventListener("click", () =>
        closeDrawer(button.closest(".drawer")),
      );
    });

    // 遮罩点击关闭抽屉
    const overlay = document.getElementById("drawerOverlay");
    if (overlay) {
      overlay.addEventListener("click", () => {
        document.querySelectorAll(".drawer.open").forEach(closeDrawer);
      });
    }

    document.addEventListener("keydown", (event) => {
      if (event.key !== "Escape") {
        return;
      }
      document.querySelectorAll(".drawer.open").forEach(closeDrawer);
    });

    enhanceSelects(document);

    // 读取服务端 flash 消息并以顶部 toast 形式展示
    const flashData = document.getElementById("flashData");
    if (flashData) {
      const ok = flashData.dataset.success;
      const err = flashData.dataset.error;
      if (ok) showToast("success", ok);
      if (err) showToast("error", err);
    }

    document.querySelectorAll("[data-password-toggle]").forEach((button) => {
      button.addEventListener("click", () => {
        const target = document.querySelector(button.dataset.passwordToggle);
        if (!target) {
          return;
        }
        target.type = target.type === "password" ? "text" : "password";
        const visible = target.type === "text";
        button.classList.toggle("is-visible", visible);
        button.setAttribute("aria-pressed", visible ? "true" : "false");
        button.setAttribute("aria-label", visible ? "隐藏密码" : "显示密码");
      });
    });
  });

  function openDrawer(drawer) {
    if (!drawer) {
      return;
    }
    drawer.classList.add("open");
    drawer.setAttribute("aria-hidden", "false");
    const overlay = document.getElementById("drawerOverlay");
    if (overlay) {
      overlay.classList.add("open");
    }
    document.body.style.overflow = "hidden";
  }

  function closeDrawer(drawer) {
    if (!drawer) {
      return;
    }
    drawer.classList.remove("open");
    drawer.setAttribute("aria-hidden", "true");
    if (!document.querySelector(".drawer.open")) {
      const overlay = document.getElementById("drawerOverlay");
      if (overlay) {
        overlay.classList.remove("open");
      }
      document.body.style.overflow = "";
    }
  }

  // ===== 自定义下拉组件：渐进增强原生 <select class="select"> =====
  // 原生 <select> 仍保留在 DOM 中（hidden via CSS），表单提交、测试断言不受影响。
  let openCustomSelect = null;

  function closeOpenCustomSelect() {
    if (openCustomSelect) {
      const wrapper = openCustomSelect;
      wrapper.classList.remove("open");
      const trigger = wrapper.querySelector(".select-trigger");
      const menu = wrapper.querySelector(".select-menu");
      if (trigger) trigger.setAttribute("aria-expanded", "false");
      if (menu) menu.setAttribute("aria-hidden", "true");
      openCustomSelect = null;
    }
  }

  function syncCustomSelect(wrapper) {
    const select = wrapper.querySelector("select.select");
    const trigger = wrapper.querySelector(".select-trigger");
    if (!select || !trigger) return;
    const opt = select.options[select.selectedIndex];
    trigger.querySelector(".select-trigger-label").textContent = opt
      ? opt.textContent
      : "";
    wrapper.querySelectorAll(".select-menu .select-option").forEach((li) => {
      const isSel = li.dataset.value === select.value;
      li.classList.toggle("selected", isSel);
      li.setAttribute("aria-selected", isSel ? "true" : "false");
    });
  }

  function buildCustomSelect(select) {
    if (select.dataset.enhanced === "1") return;
    if (select.multiple || select.size > 1) return;
    select.dataset.enhanced = "1";

    const wrapper = document.createElement("div");
    wrapper.className = "custom-select";
    if (select.disabled) wrapper.classList.add("disabled");

    const trigger = document.createElement("button");
    trigger.type = "button";
    trigger.className = "select-trigger";
    trigger.setAttribute("aria-haspopup", "listbox");
    trigger.setAttribute("aria-expanded", "false");
    if (select.disabled) trigger.disabled = true;
    trigger.innerHTML =
      '<span class="select-trigger-label"></span>' +
      '<svg class="select-trigger-caret" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>';

    const menu = document.createElement("ul");
    menu.className = "select-menu";
    menu.setAttribute("role", "listbox");
    menu.setAttribute("aria-hidden", "true");
    menu.tabIndex = -1;

    Array.from(select.options).forEach((opt, idx) => {
      const li = document.createElement("li");
      li.className = "select-option";
      li.setAttribute("role", "option");
      li.dataset.value = opt.value;
      li.dataset.index = String(idx);
      if (opt.disabled) li.classList.add("disabled");
      li.innerHTML =
        '<span class="select-option-label"></span>' +
        '<svg class="select-option-check" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
      li.querySelector(".select-option-label").textContent = opt.textContent;
      menu.appendChild(li);
    });

    select.parentNode.insertBefore(wrapper, select);
    wrapper.appendChild(select);
    wrapper.appendChild(trigger);
    wrapper.appendChild(menu);

    syncCustomSelect(wrapper);

    trigger.addEventListener("click", (e) => {
      e.stopPropagation();
      if (select.disabled) return;
      const isOpen = wrapper.classList.contains("open");
      closeOpenCustomSelect();
      if (!isOpen) {
        wrapper.classList.add("open");
        trigger.setAttribute("aria-expanded", "true");
        menu.setAttribute("aria-hidden", "false");
        openCustomSelect = wrapper;
        const sel = menu.querySelector(".select-option.selected");
        if (sel) sel.scrollIntoView({ block: "nearest" });
      }
    });

    menu.addEventListener("click", (e) => {
      const li = e.target.closest(".select-option");
      if (!li || li.classList.contains("disabled")) return;
      const newValue = li.dataset.value;
      if (select.value !== newValue) {
        select.value = newValue;
        select.dispatchEvent(new Event("change", { bubbles: true }));
      }
      syncCustomSelect(wrapper);
      closeOpenCustomSelect();
      trigger.focus();
    });

    trigger.addEventListener("keydown", (e) => {
      if (
        e.key === "ArrowDown" ||
        e.key === "ArrowUp" ||
        e.key === "Enter" ||
        e.key === " "
      ) {
        e.preventDefault();
        if (!wrapper.classList.contains("open")) {
          trigger.click();
          return;
        }
      }
      if (
        wrapper.classList.contains("open") &&
        (e.key === "ArrowDown" || e.key === "ArrowUp")
      ) {
        const options = Array.from(
          menu.querySelectorAll(".select-option:not(.disabled)"),
        );
        const cur = options.findIndex((o) => o.dataset.value === select.value);
        const next =
          e.key === "ArrowDown"
            ? Math.min(options.length - 1, cur + 1)
            : Math.max(0, cur - 1);
        const target = options[next];
        if (target) {
          select.value = target.dataset.value;
          syncCustomSelect(wrapper);
          target.scrollIntoView({ block: "nearest" });
        }
      }
    });

    select.addEventListener("change", () => syncCustomSelect(wrapper));
  }

  function enhanceSelects(root) {
    root.querySelectorAll("select.select").forEach(buildCustomSelect);
  }

  document.addEventListener("click", (e) => {
    if (!openCustomSelect) return;
    if (!openCustomSelect.contains(e.target)) {
      closeOpenCustomSelect();
    }
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && openCustomSelect) {
      closeOpenCustomSelect();
    }
  });

  // ===== Toast 通知 =====
  const TOAST_ICONS = {
    success:
      '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>',
    error:
      '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
    warn: '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
    info: '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
  };

  function ensureToastStack() {
    let stack = document.getElementById("toastStack");
    if (!stack) {
      stack = document.createElement("div");
      stack.id = "toastStack";
      stack.className = "toast-stack";
      stack.setAttribute("aria-live", "polite");
      document.body.appendChild(stack);
    }
    return stack;
  }

  function showToast(type, message, options) {
    if (!message) return;
    const safeType = TOAST_ICONS[type] ? type : "info";
    const opts = options || {};
    const duration =
      opts.duration != null
        ? opts.duration
        : safeType === "error"
          ? 5000
          : 3000;
    const stack = ensureToastStack();

    const toast = document.createElement("div");
    toast.className = "toast " + safeType;
    toast.setAttribute("role", safeType === "error" ? "alert" : "status");
    toast.innerHTML =
      TOAST_ICONS[safeType] +
      '<div class="toast-msg"></div>' +
      '<button class="toast-close" type="button" aria-label="关闭">×</button>';
    toast.querySelector(".toast-msg").textContent = message;

    const close = () => {
      toast.classList.remove("show");
      setTimeout(() => toast.remove(), 200);
    };
    toast.querySelector(".toast-close").addEventListener("click", close);
    stack.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add("show"));
    if (duration > 0) {
      setTimeout(close, duration);
    }
    return toast;
  }

  window.dbflowToast = showToast;
  window.openDrawer = openDrawer;
  window.closeDrawer = closeDrawer;
})();
