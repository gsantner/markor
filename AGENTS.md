# AGENTS.md

## Notes for contributors and agents

### String resources

- Add all user-facing strings as Android string resources. Do not hardcode UI text in Java or Kotlin.
- Add normal user-visible strings to `app/src/main/res/values/strings.xml`.
- This project keeps non-translatable string resources in `app/src/main/res/values/string-not_translatable.xml`.
- Use `translatable="false"` only for values that should not be translated, such as preference keys, internal identifiers, fixed machine-readable values, and similar non-UI text.
- Arrays with non-translatable items are defined in files like `app/src/main/res/values/arrays.xml` using `translatable="false"` on the array or items as appropriate.
- For code, reference strings via `R.string...` and `getString(...)`. For XML, use `@string/...`.
- When adding a new user-facing string, follow the existing naming/style in `strings.xml` and leave localization-ready resources in the base file rather than hardcoding text near the call site.
