# 🚀 Version Updates — Java 21 / Java 25 / Angular 22

> Tổng hợp những cải tiến quan trọng nhất của từng phiên bản  
> Giúp developer hiểu "Tại sao upgrade?" và "Feature nào nên dùng ngay?"

---

## 📁 Danh Sách File

| File | Nội Dung |
|---|---|
| [01 — Java 21 New Features](./01_java21_new_features.md) | Virtual Threads, Pattern Matching, Records, Sealed Classes, ZGC — so sánh với Java 11 & 17 |
| [02 — Java 25 New Features](./02_java25_new_features.md) | Structured Concurrency, Scoped Values, Value Objects, Stream Gatherers — LTS mới nhất 2025 |
| [03 — Angular 22 New Features](./03_angular22_new_features.md) | Zoneless, Signal Forms, Resource API, SSR Hydration — so sánh từ Angular 16 đến 22 |

---

## 🗺️ Timeline Java LTS Releases

```
Java 8  (2014) ──── LTS ────────────────────────────────────────────
Java 11 (2018) ──── LTS (Major): Var, HttpClient, String methods ───
Java 17 (2021) ──── LTS (Major): Records, Sealed Classes, Pattern ──
Java 21 (2023) ──── LTS (Major): Virtual Threads, Switch Pattern ───  ← BankX dùng
Java 25 (2025) ──── LTS (Major): Structured Concurrency, Valhalla ──  ← Tìm hiểu
Java 29 (2027) ──── LTS (dự kiến) ──────────────────────────────────
```

## 🗺️ Timeline Angular Releases (6 tháng/version)

```
Angular 14 (May 2022):    Standalone Components (preview), Typed Forms
Angular 15 (Nov 2022):    Standalone stable, Directive Composition API
Angular 16 (May 2023):    Signals (developer preview), Required Inputs
Angular 17 (Nov 2023):    @if @for @switch, defer{}, New project structure
Angular 18 (May 2024):    Zoneless (experimental), Material Design 3
Angular 19 (Nov 2024):    Incremental Hydration, linkedSignal, Resource API
Angular 20 (May 2025):    Zoneless stable (opt-in), @let syntax, Signal Forms preview
Angular 21 (Nov 2025):    Signal Forms stable, improved SSR, effect cleanup
Angular 22 (May 2026):    Full Zoneless, Signal-first architecture              ← Tìm hiểu
```

---

## 🔗 Liên Kết Với BankX

| Feature | Version | Dùng Trong BankX |
|---|---|---|
| Virtual Threads | Java 21 | `spring.threads.virtual.enabled=true` |
| Records | Java 16+ (LTS: 17) | DTO như `record TransferRequest(...)` |
| Pattern Matching `instanceof` | Java 16+ (LTS: 17/21) | `if (obj instanceof Transfer t)` |
| Switch Expression | Java 14+ | Status mapping |
| Text Blocks | Java 15+ | SQL strings trong tests |
| Standalone Components | Angular 15+ | Toàn bộ BankX frontend |
| Signals | Angular 16+ (stable 17) | `signal<>()`, `computed()` |
| `@if @for @switch` | Angular 17+ | Template syntax mới |
| `inject()` | Angular 14+ | DI pattern thay `constructor` |
