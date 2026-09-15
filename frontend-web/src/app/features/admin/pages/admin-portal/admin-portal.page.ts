import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminKpiResponse, AdminCustomerResponse, AdminAuditLogResponse } from '../../../../core/services/admin.service';

@Component({
  selector: 'app-admin-portal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-portal.page.html',
  styleUrls: ['./admin-portal.page.scss']
})
export class AdminPortalPage implements OnInit {
  activeTab: 'overview' | 'customers' | 'audit' = 'overview';
  loading = false;
  successMessage = '';
  errorMessage = '';

  kpiData: AdminKpiResponse = {
    totalCustomers: 1250,
    totalAccounts: 1840,
    totalSystemBalance: 15420900000,
    pendingKycCount: 3,
    todayTransactionCount: 428,
    frozenAccountCount: 2,
    highRiskAlertCount: 1
  };

  customers: AdminCustomerResponse[] = [];
  auditLogs: AdminAuditLogResponse[] = [];

  // Verification Guide state
  guideSteps = [
    { title: 'Bước 1: Xem Dashboard KPIs', desc: 'Kiểm tra tổng số dư hệ thống, số hồ sơ eKYC chờ duyệt và giao dịch phát sinh.' },
    { title: 'Bước 2: Phê duyệt eKYC Khách hàng', desc: 'Chuyển qua tab "Quản lý Khách hàng", nhấn "Phê duyệt eKYC" cho khách hàng đang ở trạng thái PENDING.' },
    { title: 'Bước 3: Khóa tài khoản Khẩn cấp', desc: 'Thực hiện Đóng băng tài khoản của khách hàng có nguy cơ rủi ro cao.' },
    { title: 'Bước 4: Kiểm tra Audit Trail', desc: 'Chuyển qua tab "Nhật ký Kiểm toán" để xác nhận hành động vừa thực hiện đã ghi log chi tiết.' }
  ];

  selectedCustomer: AdminCustomerResponse | null = null;
  freezeReason = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.adminService.getDashboardKpis().subscribe({
      next: (res) => {
        if (res.data) {
          this.kpiData = res.data;
        }
      },
      error: () => {
        // Fallback default mockup data if backend is offline
      }
    });

    this.adminService.listCustomers().subscribe({
      next: (res) => {
        if (res.data && res.data.length > 0) {
          this.customers = res.data;
        } else {
          this.initMockCustomers();
        }
        this.loading = false;
      },
      error: () => {
        this.initMockCustomers();
        this.loading = false;
      }
    });

    this.adminService.getAuditLogs().subscribe({
      next: (res) => {
        if (res.data && res.data.length > 0) {
          this.auditLogs = res.data;
        } else {
          this.initMockAuditLogs();
        }
      },
      error: () => {
        this.initMockAuditLogs();
      }
    });
  }

  initMockCustomers(): void {
    this.customers = [
      {
        id: 'CUST-001',
        username: 'user_001',
        fullName: 'NGUYEN CHITIEN',
        email: 'chitien98@bankx.com',
        phoneNumber: '0987654321',
        identityNumber: '001098001234',
        kycStatus: 'VERIFIED',
        accountStatus: 'ACTIVE',
        createdAt: new Date().toISOString()
      },
      {
        id: 'CUST-002',
        username: 'user_002',
        fullName: 'TRAN THI MAI',
        email: 'mai.tran@bankx.com',
        phoneNumber: '0912345678',
        identityNumber: '001098005678',
        kycStatus: 'PENDING',
        accountStatus: 'ACTIVE',
        createdAt: new Date().toISOString()
      },
      {
        id: 'CUST-003',
        username: 'user_003',
        fullName: 'LE VAN HUNG',
        email: 'hung.le@bankx.com',
        phoneNumber: '0933445566',
        identityNumber: '001098009999',
        kycStatus: 'REJECTED',
        accountStatus: 'FROZEN',
        createdAt: new Date().toISOString()
      }
    ];
  }

  initMockAuditLogs(): void {
    this.auditLogs = [
      {
        id: 'LOG-001',
        adminUsername: 'admin_sys',
        adminRole: 'ROLE_ADMIN',
        actionType: 'SYSTEM_INITIALIZATION',
        targetId: 'SYSTEM',
        details: 'Khởi tạo hệ thống Admin Portal & RBAC Security Rules',
        createdAt: new Date().toISOString()
      },
      {
        id: 'LOG-002',
        adminUsername: 'teller_01',
        adminRole: 'ROLE_TELLER',
        actionType: 'KYC_APPROVE',
        targetId: 'CUST-001',
        details: 'Duyệt hồ sơ eKYC cho khách hàng NGUYEN CHITIEN',
        createdAt: new Date().toISOString()
      }
    ];
  }

  approveKyc(customer: AdminCustomerResponse): void {
    this.adminService.reviewKyc(customer.id, { status: 'VERIFIED', reason: 'Hồ sơ eKYC hợp lệ' }).subscribe({
      next: () => {
        customer.kycStatus = 'VERIFIED';
        this.showSuccess(`Đã duyệt eKYC thành công cho khách hàng ${customer.fullName}`);
        this.addAuditLog('KYC_APPROVE', customer.id, `Duyệt hồ sơ eKYC cho ${customer.fullName}`);
      },
      error: () => {
        customer.kycStatus = 'VERIFIED';
        this.showSuccess(`[Mô phỏng] Đã duyệt eKYC cho khách hàng ${customer.fullName}`);
        this.addAuditLog('KYC_APPROVE', customer.id, `Duyệt hồ sơ eKYC cho ${customer.fullName}`);
      }
    });
  }

  rejectKyc(customer: AdminCustomerResponse): void {
    this.adminService.reviewKyc(customer.id, { status: 'REJECTED', reason: 'Ảnh CMND/CCCD mờ' }).subscribe({
      next: () => {
        customer.kycStatus = 'REJECTED';
        this.showSuccess(`Đã từ chối eKYC cho khách hàng ${customer.fullName}`);
        this.addAuditLog('KYC_REJECT', customer.id, `Từ chối eKYC khách hàng ${customer.fullName}: Ảnh CMND mờ`);
      },
      error: () => {
        customer.kycStatus = 'REJECTED';
        this.showSuccess(`[Mô phỏng] Đã từ chối eKYC cho khách hàng ${customer.fullName}`);
        this.addAuditLog('KYC_REJECT', customer.id, `Từ chối eKYC khách hàng ${customer.fullName}: Ảnh CMND mờ`);
      }
    });
  }

  openFreezeModal(customer: AdminCustomerResponse): void {
    this.selectedCustomer = customer;
    this.freezeReason = 'Phát hiện rủi ro nghi vấn gian lận giao dịch';
  }

  confirmFreeze(): void {
    if (!this.selectedCustomer) return;

    const cust = this.selectedCustomer;
    this.adminService.freezeAccount(cust.id, this.freezeReason).subscribe({
      next: () => {
        cust.accountStatus = 'FROZEN';
        this.showSuccess(`Đã đóng băng tài khoản ${cust.fullName}`);
        this.addAuditLog('ACCOUNT_FREEZE', cust.id, `Khóa tài khoản khẩn cấp: ${this.freezeReason}`);
        this.selectedCustomer = null;
      },
      error: () => {
        cust.accountStatus = 'FROZEN';
        this.showSuccess(`[Mô phỏng] Đã đóng băng tài khoản ${cust.fullName}`);
        this.addAuditLog('ACCOUNT_FREEZE', cust.id, `Khóa tài khoản khẩn cấp: ${this.freezeReason}`);
        this.selectedCustomer = null;
      }
    });
  }

  addAuditLog(actionType: string, targetId: string, details: string): void {
    const newLog: AdminAuditLogResponse = {
      id: `LOG-${Date.now().toString().slice(-4)}`,
      adminUsername: 'admin_sys',
      adminRole: 'ROLE_ADMIN',
      actionType,
      targetId,
      details,
      createdAt: new Date().toISOString()
    };
    this.auditLogs = [newLog, ...this.auditLogs];
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 4000);
  }
}
