# Development and distribution

- Native Android, local storage, and no account, ads, server, or paid API.
- Cohesive tested commits on main; no force pushes.
- Pushes build signed debug APKs. Release publication is separate.
- Debug and release distributions use the certificate pinned in config/signing-certificate.sha256. Missing credentials fail packaging; they never trigger key replacement.
- Responsive layouts, system font scaling, dark mode, and finite animation apply across supported phones.
- README screenshots must match the current interface. Public documentation stays product-focused.
