import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FraudService, FraudEvaluationResult, FraudAlert, FraudRule, EvaluateTransactionRequest } from '../../../../core/services/fraud.service';

@Component({
  selector: 'app-fraud-dashboard-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fraud-dashboard.page.html',
  styleUrls: ['./fraud-dashboard.page.scss']
})
export class FraudDashboardPage implements OnInit {
  activeTab: 'simulator' | 'alerts' | 'rules' = 'simulator';

  loading = false;
  errorMessage = '';
  successMessage = '';

  // Simulator Form
  simSourceAccountId = '1088889999';
  simTargetAccountNumber = '9988776655';
  simAmount = 120000000;
  simIsNewDevice = true;
  simIsNewBeneficiary = true;
  simVelocity = 6;
  simHour = 2; // 2 AM (Night time)

  evalResult: FraudEvaluationResult | null = null;

  // Alerts List & Review Modal
  alerts: FraudAlert[] = [];
  selectedAlert: FraudAlert | null = null;
  showReviewModal = false;
  reviewStatus: 'RESOLVED' | 'FALSE_POSITIVE' = 'RESOLVED';
  reviewNotes = '';

  // Rules List
  rules: FraudRule[] = [];

  constructor(private fraudService: FraudService) {}

  ngOnInit(): void {
    this.evaluateSimulation();
    this.loadAlerts();
    this.loadRules();
  }

  evaluateSimulation(): void {
    this.loading = true;
    this.errorMessage = '';

    const req: EvaluateTransactionRequest = {
      sourceAccountId: this.simSourceAccountId,
      targetAccountNumber: this.simTargetAccountNumber,
      amount: this.simAmount,
      isNewDevice: this.simIsNewDevice,
      isNewBeneficiary: this.simIsNewBeneficiary,
      velocityLastMinute: this.simVelocity,
      customHour: this.simHour
    };

    this.fraudService.evaluateTransaction(req).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.data) {
          this.evalResult = res.data;
          this.loadAlerts(); // Refresh alerts list if blocked alert was recorded
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Lỗi khi mô phỏng đánh giá gian lận';
      }
    });
  }

  loadAlerts(): void {
    this.fraudService.getAlerts().subscribe({
      next: (res) => {
        if (res.data) {
          this.alerts = res.data;
        }
      },
      error: (err: any) => {
        console.error('Lỗi tải danh sách cảnh báo gian lận:', err);
      }
    });
  }

  loadRules(): void {
    this.fraudService.getRules().subscribe({
      next: (res) => {
        if (res.data) {
          this.rules = res.data;
        }
      },
      error: (err: any) => {
        console.error('Lỗi tải danh sách quy tắc gian lận:', err);
      }
    });
  }

  openReviewModal(alert: FraudAlert): void {
    this.selectedAlert = alert;
    this.reviewStatus = 'RESOLVED';
    this.reviewNotes = '';
    this.showReviewModal = true;
  }

  submitReview(): void {
    if (!this.selectedAlert) return;

    this.loading = true;
    this.fraudService.reviewAlert(this.selectedAlert.id, this.reviewStatus, this.reviewNotes).subscribe({
      next: (res) => {
        this.loading = false;
        this.showReviewModal = false;
        if (res.data) {
          this.successMessage = `Đã cập nhật trạng thái cảnh báo [${res.data.id}] sang ${res.data.status}`;
          this.loadAlerts();
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Duyệt cảnh báo thất bại';
      }
    });
  }

  toggleRuleActive(rule: FraudRule): void {
    const newActiveState = !rule.isActive;
    this.fraudService.toggleRule(rule.id, newActiveState).subscribe({
      next: (res) => {
        if (res.data) {
          rule.isActive = res.data.isActive;
          this.successMessage = `Đã ${rule.isActive ? 'Bật' : 'Tắt'} quy tắc [${rule.ruleName}]`;
          this.evaluateSimulation();
        }
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || 'Cập nhật trạng thái quy tắc thất bại';
      }
    });
  }
}
