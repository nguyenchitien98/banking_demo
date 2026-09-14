import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Layout chính của BankX Web App (Banking Customer Portal).
 *
 * Chứa: Top header + Bottom navigation (mobile) + Router outlet.
 * Sẽ được thiết kế đầy đủ theo TPBank UI ở Sprint 05.
 */
@Component({
  selector: 'bankx-main-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="bankx-layout">
      <!-- Header và Navigation sẽ thêm ở Sprint 05 -->
      <main class="bankx-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .bankx-layout { min-height: 100vh; display: flex; flex-direction: column; }
    .bankx-content { flex: 1; }
  `]
})
export class MainLayoutComponent {}
