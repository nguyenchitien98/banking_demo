# Bài 02 — TypeScript Cơ Bản & Interface (Qua Lăng Kính BankX)

> **Mục tiêu:** Hiểu TypeScript đủ để đọc và viết code Angular trong BankX  
> **File thực tế:** `account.service.ts`, `auth.service.ts`, `transfer.service.ts`

---

## 1. TypeScript là gì? Tại sao Angular dùng TS?

TypeScript = JavaScript + **kiểu dữ liệu tĩnh (Static Types)**

```typescript
// JavaScript (không có kiểu) — dễ lỗi runtime
function tinhLai(soTien, laiSuat) {
  return soTien * laiSuat; // Nếu ai đó truyền "abc" thì sao?
}

// TypeScript (có kiểu) — bắt lỗi lúc compile
function tinhLai(soTien: number, laiSuat: number): number {
//                ↑ kiểu số   ↑ kiểu số              ↑ kiểu trả về
  return soTien * laiSuat;
}

tinhLai("abc", 0.05); // ❌ LỖI ngay lúc viết code, không cần chạy mới biết
tinhLai(1000000, 0.05); // ✅ OK
```

---

## 2. Các Kiểu Dữ Liệu Cơ Bản trong TypeScript

```typescript
// ===== KIỂU CƠ BẢN =====

let ten: string = 'Nguyễn Văn A';     // Chuỗi văn bản
let soTien: number = 5000000;          // Số (cả số nguyên lẫn số thực)
let daXacThuc: boolean = true;         // Đúng/sai
let nguoiDung: null = null;            // Không có giá trị (chủ động)
let token: undefined = undefined;      // Chưa được gán giá trị


// ===== MẢNG (Array) =====

let danhSachTaiKhoan: string[] = ['088880001', '088880002'];
// Hoặc viết:
let danhSachSoTien: Array<number> = [100000, 500000, 1000000];


// ===== UNION TYPE (hoặc kiểu này, hoặc kiểu kia) =====
// Dùng nhiều trong BankX:

let trangThai: 'ACTIVE' | 'FROZEN' | 'BLOCKED'; // Chỉ được là 1 trong 3 giá trị
trangThai = 'ACTIVE';  // ✅
trangThai = 'PENDING'; // ❌ Lỗi compile


// ===== ANY — tắt kiểm tra kiểu (hạn chế dùng) =====

let batKy: any = 'abc';
batKy = 123;     // Không báo lỗi
batKy = true;    // Không báo lỗi
// any giống JavaScript thuần, mất lợi ích của TypeScript


// ===== GENERIC TYPE — kiểu tổng quát =====
// Dùng rất nhiều trong BankX với ApiResponse<T>

// T là "placeholder" — sẽ thay thế bằng kiểu cụ thể khi dùng
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;           // T có thể là BankAccount, Transfer, bất kỳ gì
  timestamp: string;
}

// Ví dụ sử dụng:
let response1: ApiResponse<BankAccount>;   // data là BankAccount
let response2: ApiResponse<BankAccount[]>; // data là mảng BankAccount
let response3: ApiResponse<string>;        // data là chuỗi
```

---

## 3. Interface — Khai Báo "Hình Dạng" Dữ Liệu

Interface giống như **hợp đồng dữ liệu** — định nghĩa một object phải có những field nào, kiểu gì.

### Ví dụ thực tế từ BankX:

```typescript
// File: frontend-web/src/app/core/services/account.service.ts

// Interface định nghĩa "hình dạng" của một Tài khoản Ngân hàng
export interface BankAccount {
  id: string;                          // Mã định danh nội bộ (UUID)
  customerId: string;                  // Mã khách hàng sở hữu tài khoản
  accountNumber: string;               // Số tài khoản hiển thị (VD: "088880001")
  accountName: string;                 // Tên tài khoản (VD: "Tài khoản Thanh toán")
  balance: number;                     // Số dư (đơn vị: VND)
  currency: string;                    // Đơn vị tiền tệ (VD: "VND")
  status: 'ACTIVE' | 'FROZEN' | string; // Trạng thái tài khoản
  version: number;                     // Số phiên bản (Optimistic Lock)
}

// Dùng interface:
const taiKhoan: BankAccount = {
  id: 'acc-001',
  customerId: 'cust-001',
  accountNumber: '088880001',
  accountName: 'Tài khoản Thanh toán',
  balance: 5000000,
  currency: 'VND',
  status: 'ACTIVE',
  version: 1
};

// Nếu thiếu field → LỖI compile ngay:
const loi: BankAccount = {
  id: 'acc-002'
  // ❌ Thiếu các field bắt buộc → TypeScript báo lỗi
};
```

### Interface với Optional Fields (`?`):

```typescript
// Interface cho Request Tạo Tài khoản
export interface CreateAccountRequest {
  accountName: string;        // Bắt buộc (không có ?)
  initialBalance?: number;    // Tùy chọn (có ?) — có thể không truyền
}

// Dùng:
const req1: CreateAccountRequest = {
  accountName: 'Tài khoản tiết kiệm',
  initialBalance: 1000000     // Có hoặc không đều OK
};

const req2: CreateAccountRequest = {
  accountName: 'Tài khoản tiết kiệm'
  // initialBalance không truyền cũng không lỗi
};
```

### Interface Lồng Nhau:

```typescript
// File: auth.service.ts

// Interface cho User trong hệ thống
export interface UserSummary {
  id: string;
  username: string;
  email: string;
  roles: string[];    // Mảng string (VD: ['CUSTOMER', 'ADMIN'])
}

// Interface cho Response Đăng nhập — LỒNG UserSummary vào trong
export interface LoginResponse {
  accessToken: string;   // JWT Access Token
  refreshToken: string;  // Refresh Token
  tokenType: string;     // Loại token (VD: "Bearer")
  expiresIn: number;     // Thời gian hết hạn (giây)
  user: UserSummary;     // ← LỒNG interface UserSummary vào đây
}

// Interface chung cho MỌI response từ Backend BankX
// T là Generic Type — thay bằng kiểu dữ liệu thực tế khi dùng
export interface ApiResponse<T> {
  code: number;           // Mã kết quả (0 = thành công)
  message: string;        // Thông báo từ server
  data: T;                // Dữ liệu thực sự (kiểu Generic)
  timestamp: string;      // Thời gian server xử lý (ISO 8601)
  traceId: string;        // ID để theo dõi request (Distributed Tracing)
}

// Ví dụ dùng ApiResponse<T>:
// API login trả về ApiResponse<LoginResponse>
// data bên trong là LoginResponse
let loginRes: ApiResponse<LoginResponse>;
loginRes.data.accessToken; // ✅ TypeScript biết data là LoginResponse
loginRes.data.user.username; // ✅ TypeScript biết user là UserSummary
```

---

## 4. Type Alias — Tạo Tên Ngắn Gọn Cho Kiểu

```typescript
// type alias tạo tên mới cho một kiểu
type AccountStatus = 'ACTIVE' | 'FROZEN' | 'BLOCKED';
type TransferStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
type CurrencyCode = 'VND' | 'USD' | 'EUR';

// Dùng:
let trangThai: AccountStatus = 'ACTIVE';   // ✅
trangThai = 'PENDING';                      // ❌ Lỗi — không có trong AccountStatus
```

---

## 5. Khai Báo Biến: `const`, `let`, `var`

```typescript
// const — Không thể gán lại (Immutable reference)
const API_URL = '/api/v1/accounts';  // Hằng số, KHÔNG bao giờ thay đổi
API_URL = '/api/v2/accounts';        // ❌ Lỗi — không được gán lại

// Nhưng object/array khai báo const VẪN có thể thay đổi NỘI DUNG:
const accounts: BankAccount[] = [];
accounts.push(newAccount);   // ✅ OK — push vào mảng được
accounts = [];               // ❌ Lỗi — không gán lại biến accounts


// let — Có thể gán lại (trong cùng block scope)
let loading = true;
loading = false;   // ✅ OK


// var — CŨ, không dùng nữa (có vấn đề scope)
// Trong BankX không dùng var, chỉ dùng const và let
```

---

## 6. Arrow Function — Cú Pháp Hàm Ngắn Gọn

```typescript
// Cách truyền thống:
function cong(a: number, b: number): number {
  return a + b;
}

// Arrow function (cú pháp =>):
const cong = (a: number, b: number): number => {
  return a + b;
};

// Arrow function rút gọn (1 dòng, không cần return):
const cong = (a: number, b: number): number => a + b;


// ===== DÙNG TRONG BANKX =====

// accounts.page.ts — Arrow function trong subscribe
this.accountService.getMyAccounts().subscribe({
  next: (res) => {                    // next là arrow function nhận response
    this.accounts.set(res.data);      // xử lý khi thành công
  },
  error: (err) => {                   // error là arrow function nhận lỗi
    console.error(err);               // xử lý khi lỗi
  }
});

// Array methods với arrow function:
const taiKhoanActive = accounts.filter(
  (acc) => acc.status === 'ACTIVE'   // Lọc chỉ tài khoản ACTIVE
);

const soTienList = accounts.map(
  (acc) => acc.balance               // Lấy ra mảng chỉ có số dư
);
```

---

## 7. Optional Chaining (`?.`) — Truy Cập An Toàn

```typescript
// Vấn đề: currentUser có thể là null
const user = currentUser; // user có thể là null hoặc UserSummary

// Cách cũ (dài, xấu):
if (user !== null && user !== undefined) {
  console.log(user.username);
}

// Optional Chaining (?.) — GỌN hơn nhiều:
console.log(user?.username); // Nếu user là null → trả về undefined thay vì lỗi


// ===== DÙNG TRONG BANKX =====

// main-layout.component.html:
// {{ currentUser()?.username || 'Khách hàng' }}
//               ↑ Optional chaining — an toàn khi currentUser() trả về null
//                               ↑ Nullish coalescing — nếu null thì dùng 'Khách hàng'

// main-layout.component.ts:
getUserInitials(): string {
  const username = this.currentUser()?.username || 'U';
  //                               ↑ An toàn — nếu currentUser null thì username là 'U'
  return username.substring(0, 2).toUpperCase();
}
```

---

## 8. Template Literals (Backtick String)

```typescript
// Cách cũ:
const message = 'Xin chào ' + username + '! Số dư: ' + balance + ' VND';

// Template Literal (dùng backtick ` và ${...}):
const message = `Xin chào ${username}! Số dư: ${balance} VND`;
//               ↑ backtick    ↑ biến được nhúng trực tiếp


// ===== DÙNG TRONG BANKX =====
// accounts.page.ts:
this.showToast(`Hạch toán thành công! Mã GD: ${res.data.transactionReference}`);

// account.service.ts:
return this.http.patch(`${this.API_URL}/${accountId}/freeze`, {});
//                      ↑ URL động — accountId được nhúng vào chuỗi
```

---

## 9. Destructuring — Giải Cấu Trúc

```typescript
// Object Destructuring — lấy field từ object ra thành biến riêng
const account: BankAccount = {
  id: 'acc-001',
  accountNumber: '088880001',
  balance: 5000000,
  // ...
};

// Cách cũ:
const id = account.id;
const balance = account.balance;

// Destructuring:
const { id, balance, accountNumber } = account;
// ↑ id, balance, accountNumber đã là biến riêng


// Array Destructuring — dùng trong forkJoin BankX:
// accounts.page.ts:
forkJoin([req1, req2]).subscribe({
  next: ([res1, res2]) => {   // ← Destructuring array result
    // res1 = kết quả của req1
    // res2 = kết quả của req2
  }
});
```

---

## 10. `private`, `public`, `readonly` — Access Modifiers

```typescript
// Ví dụ từ BankX: accounts.page.ts

export class AccountsPage {
  // private: Chỉ class này mới truy cập được
  // readonly: Không thể gán lại sau khi khởi tạo
  private readonly accountService = inject(AccountService);
  //      ↑ chỉ dùng trong class    ↑ không ai thay đổi được reference

  // public: Mọi nơi đều truy cập được (HTML template cũng dùng được)
  public accounts = signal<BankAccount[]>([]);

  // Không viết gì = public (mặc định)
  loadingAccounts = true;


  // private method — chỉ dùng trong class
  private showToast(msg: string): void {
    this.toastMessage = msg;
  }

  // public method — HTML template gọi được
  public loadAccounts(): void {
    this.accountService.getMyAccounts().subscribe(/* ... */);
  }
}
```

**Quy tắc BankX:**
- `private readonly` → Services được inject (accountService, ledgerService...)
- `public` → Properties mà HTML template cần đọc (accounts, loading...)
- `private` → Methods nội bộ chỉ class dùng (showToast, calculateProof...)

---

## 11. Tổng Kết

| Khái Niệm | Ví Dụ | File BankX |
|---|---|---|
| `interface` | `BankAccount`, `ApiResponse<T>` | `account.service.ts` |
| Generic `<T>` | `ApiResponse<BankAccount>` | `auth.service.ts` |
| Optional `?` | `initialBalance?: number` | `account.service.ts` |
| Union Type | `'ACTIVE' \| 'FROZEN'` | `account.service.ts` |
| Arrow Function | `(res) => { ... }` | `accounts.page.ts` |
| Optional Chaining | `currentUser()?.username` | `main-layout.component.ts` |
| Template Literal | `` `${API_URL}/${id}` `` | `account.service.ts` |
| `private readonly` | `private readonly http = inject(...)` | `account.service.ts` |

---

**← [Bài 01 — Angular CLI](./01_angular_cli_va_cau_truc.md)** | **→ [Bài 03 — Component & Lifecycle](./03_component_va_lifecycle.md)**
