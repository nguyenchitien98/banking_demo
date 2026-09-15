import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SagaService, SagaInstanceResponse, SagaExecutionRequest } from '../../../../core/services/saga.service';

@Component({
  selector: 'app-saga-orchestration',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './saga-orchestration.page.html',
  styleUrls: ['./saga-orchestration.page.scss']
})
export class SagaOrchestrationPage implements OnInit {
  loading = false;
  executing = false;
  successMessage = '';
  errorMessage = '';

  request: SagaExecutionRequest = {
    sourceAccountNumber: '1000188888',
    targetAccountNumber: '1000299999',
    amount: 1500000,
    description: 'Chuyển tiền liên ngân hàng qua Saga Orchestrator',
    forceFailureStep: 'NONE'
  };

  activeSaga: SagaInstanceResponse | null = null;
  sagasList: SagaInstanceResponse[] = [];

  // Verification Guide Panel
  guideSteps = [
    { title: 'Bước 1: Khởi Chạy Happy Path (NONE)', desc: 'Chọn cờ lỗi `NONE` và nhấn "Khởi Chạy Saga". Kiểm tra luồng trạng thái COMPLETED 100%.' },
    { title: 'Bước 2: Giả Lập Lỗi Credit (InterBank)', desc: 'Đổi cờ thành `CREDIT_FAILED` và nhấn Khởi Chạy. Quan sát State Machine chuyển sang FAILED_COMPENSATED.' },
    { title: 'Bước 3: Kiểm Tra Bút Toán Đảo (REVERSE_DEBIT)', desc: 'Xác minh trong bảng Audit Step đã tự động phát lệnh `REVERSE_DEBIT` để hoàn tiền về STK nguồn.' },
    { title: 'Bước 4: Tra Cứu Nhật Ký State Machine', desc: 'Kiểm tra danh sách lịch sử các Saga Instances đã lưu vết trong DB `saga_instances`.' }
  ];

  constructor(private sagaService: SagaService) {}

  ngOnInit(): void {
    this.loadSagas();
  }

  loadSagas(): void {
    this.loading = true;
    this.sagaService.listSagas().subscribe({
      next: (res) => {
        if (res.data && res.data.length > 0) {
          this.sagasList = res.data;
          this.activeSaga = res.data[0];
        } else {
          this.initMockData();
        }
        this.loading = false;
      },
      error: () => {
        this.initMockData();
        this.loading = false;
      }
    });
  }

  executeSaga(): void {
    this.executing = true;
    this.errorMessage = '';
    this.sagaService.executeSaga(this.request).subscribe({
      next: (res) => {
        if (res.data) {
          this.activeSaga = res.data;
          this.sagasList = [res.data, ...this.sagasList];
          if (res.data.currentState === 'COMPLETED') {
            this.showSuccess(`[Saga Success] Giao dịch ${res.data.transferCode} thành công 100%!`);
          } else {
            this.showSuccess(`[Saga Compensated] Giao dịch ${res.data.transferCode} gặp lỗi -> Đã hoàn tất Bút toán Đảo!`);
          }
        }
        this.executing = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Lỗi khi thực thi Saga Orchestrator';
        this.executing = false;
      }
    });
  }

  selectSaga(saga: SagaInstanceResponse): void {
    this.activeSaga = saga;
  }

  initMockData(): void {
    const mock: SagaInstanceResponse = {
      sagaId: 'SAGA-1001',
      transferCode: 'SAGA-TRF-001',
      currentState: 'COMPLETED',
      sourceAccountNumber: '1000188888',
      targetAccountNumber: '1000299999',
      amount: 2000000,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      steps: [
        { id: 'STEP-001', sagaId: 'SAGA-1001', stepName: 'INITIATE_SAGA', stateBefore: 'NOT_STARTED', stateAfter: 'STARTED', compensating: false, details: 'Khởi tạo Saga Instance', createdAt: new Date().toISOString() },
        { id: 'STEP-002', sagaId: 'SAGA-1001', stepName: 'EXECUTE_DEBIT', stateBefore: 'STARTED', stateAfter: 'DEBIT_COMPLETED', compensating: false, details: 'Trừ tiền tài khoản nguồn 1000188888', createdAt: new Date().toISOString() },
        { id: 'STEP-003', sagaId: 'SAGA-1001', stepName: 'EXECUTE_CREDIT', stateBefore: 'DEBIT_COMPLETED', stateAfter: 'CREDIT_COMPLETED', compensating: false, details: 'Cộng tiền tài khoản đích 1000299999', createdAt: new Date().toISOString() },
        { id: 'STEP-004', sagaId: 'SAGA-1001', stepName: 'RECORD_LEDGER', stateBefore: 'CREDIT_COMPLETED', stateAfter: 'COMPLETED', compensating: false, details: 'Hạch toán bút toán ghi sổ kép', createdAt: new Date().toISOString() }
      ]
    };
    this.sagasList = [mock];
    this.activeSaga = mock;
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 4000);
  }
}
