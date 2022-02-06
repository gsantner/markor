(() => {
    let divs = document.getElementsByClassName("adm-block");
    for (let i = 0; i < divs.length; i++) {
        let div = divs[i];
        if (div.classList.contains("adm-collapsed") || div.classList.contains("adm-open")) {
            let headings = div.getElementsByClassName("adm-heading");
            if (headings.length > 0) {
                headings[0].addEventListener("click", event => {
                    let el = div;
                    event.preventDefault();
                    event.stopImmediatePropagation();
                    if (el.classList.contains("adm-collapsed")) {
                        console.debug("Admonition Open", event.srcElement);
                        el.classList.remove("adm-collapsed");
                        el.classList.add("adm-open");
                    } else {
                        console.debug("Admonition Collapse", event.srcElement);
                        el.classList.add("adm-collapsed");
                        el.classList.remove("adm-open");
                    }
                });
            }
        }
    }
})();
