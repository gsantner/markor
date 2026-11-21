(() => {
    "use strict";

    const preview = document.getElementById("markor-todo");
    const rows = document.getElementById("todo-rows");
    const filter = document.getElementById("todo-filter");
    const status = document.getElementById("todo-status");
    const empty = document.getElementById("todo-empty");
    let revision = preview.dataset.revision;
    let press;

    function send(...args) {
        Android.webViewJavascriptCallback(["todotxt-preview", revision, ...args]);
    }

    function applyFilter() {
        const terms = filter.value.trim().toLowerCase().split(/\s+/).filter(Boolean);
        let hasMatches = false;
        for (const row of rows.children) {
            const done = row.classList.contains("todo-done");
            const matchesStatus = status.value === "all" || done === (status.value === "done");
            const text = row.textContent.toLowerCase();
            row.hidden = !matchesStatus || !terms.every(term => text.includes(term));
            hasMatches = hasMatches || !row.hidden;
        }
        empty.hidden = hasMatches;
    }

    function cancelPress() {
        if (press) {
            clearTimeout(press.timer);
            press = null;
        }
    }

    rows.addEventListener("pointerdown", event => {
        cancelPress();
        const task = event.target.closest(".todo-text");
        if (task && event.isPrimary && event.button === 0) {
            const line = task.parentElement.dataset.line;
            press = {
                id: event.pointerId,
                x: event.clientX,
                y: event.clientY,
                timer: setTimeout(() => {
                    cancelPress();
                    send("edit", line);
                }, 500)
            };
        }
    });
    window.addEventListener("pointermove", event => {
        if (press && event.pointerId === press.id && Math.hypot(event.clientX - press.x, event.clientY - press.y) > 10) {
            cancelPress();
        }
    });
    window.addEventListener("pointerup", cancelPress);
    window.addEventListener("pointercancel", cancelPress);
    window.addEventListener("blur", cancelPress);
    rows.addEventListener("contextmenu", event => {
        if (event.target.closest(".todo-text")) {
            event.preventDefault();
        }
    });

    rows.addEventListener("click", event => {
        const target = event.target;
        if (target.matches("input[type=checkbox]")) {
            // Apply the checked state when Markor confirms the source edit.
            event.preventDefault();
            send("done", target.closest(".todo-row").dataset.line, String(target.checked));
        } else if (event.detail === 0) {
            const task = target.closest(".todo-text");
            if (task) {
                send("edit", task.parentElement.dataset.line);
            }
        }
    });
    document.getElementById("todo-sort").addEventListener("click", () => send("sort"));
    filter.addEventListener("input", applyFilter);
    status.addEventListener("change", applyFilter);

    window.markorTodo = {
        focusFilter() {
            filter.focus();
            return document.activeElement === filter;
        },

        updateRow(expectedRevision, line, html) {
            if (expectedRevision !== revision) {
                return;
            }
            const row = rows.querySelector(`[data-line="${line}"]`);
            if (!row) {
                return;
            }
            cancelPress();
            const checkboxHadFocus = row.querySelector("input") === document.activeElement;
            let updated;
            if (html) {
                row.insertAdjacentHTML("beforebegin", html);
                updated = row.previousElementSibling;
            }
            row.remove();
            applyFilter();
            if (checkboxHadFocus && updated && !updated.hidden) {
                updated.querySelector("input").focus({preventScroll: true});
            }
        },

        updateRows(expectedRevision, nextRevision, html) {
            if (expectedRevision !== revision) {
                return;
            }
            cancelPress();
            const scrollY = window.scrollY;
            revision = nextRevision;
            rows.innerHTML = html;
            applyFilter();
            window.scrollTo(window.scrollX, scrollY);
        }
    };

    applyFilter();
})();
