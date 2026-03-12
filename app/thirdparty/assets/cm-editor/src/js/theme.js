export function isDarkMode() {
    let content = document.querySelector('meta[name="theme"]').getAttribute('content');
    return content === "dark";
}
