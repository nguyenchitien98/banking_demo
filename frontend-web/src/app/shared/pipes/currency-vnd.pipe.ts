import { Pipe, PipeTransform } from '@angular/core';

/**
 * Pipe định dạng số tiền VND theo chuẩn TPBank.
 *
 * Ví dụ:
 * - 145000000 → "145.000.000 ₫"
 * - 500000 → "500.000 ₫"
 * - 0 → "0 ₫"
 * - null/undefined → "---"
 *
 * Sử dụng Intl.NumberFormat thay vì tự format tay để:
 * - Đúng theo locale (vi-VN dùng dấu chấm phân cách nghìn)
 * - Không bị lỗi float point
 */
@Pipe({
  name: 'currencyVnd',
  standalone: true,
  pure: true,  // Pure pipe: chỉ re-compute khi input thay đổi (performance)
})
export class CurrencyVndPipe implements PipeTransform {
  private readonly formatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });

  /**
   * Định dạng số tiền VND.
   *
   * @param value Số tiền (number, string hoặc null/undefined)
   * @param fallback Giá trị hiển thị khi value null (mặc định "---")
   * @returns Chuỗi tiền tệ đã format, ví dụ "145.000.000 ₫"
   */
  transform(value: number | string | null | undefined, fallback = '---'): string {
    if (value === null || value === undefined || value === '') {
      return fallback;
    }
    const num = typeof value === 'string' ? parseFloat(value) : value;
    if (isNaN(num)) {
      return fallback;
    }
    return this.formatter.format(num);
  }
}
