# 🎨 UI Integration Guide - Admin Notifications

## **Overview**
This guide shows you what UI changes to make in your frontend to integrate the admin notification system.

---

## **🔔 Notification Components to Add**

### **1. Notification Bell Icon (Header/Navbar)**
```html
<!-- Add this to your admin header/navbar -->
<div class="notification-bell">
  <i class="fas fa-bell"></i>
  <span class="notification-badge" *ngIf="unreadCount > 0">{{unreadCount}}</span>
  <div class="notification-dropdown" *ngIf="showNotifications">
    <div class="notification-item" *ngFor="let notification of notifications">
      <div class="notification-title">{{notification.title}}</div>
      <div class="notification-message">{{notification.message}}</div>
      <div class="notification-time">{{notification.createdAt | date:'short'}}</div>
    </div>
    <div class="notification-footer">
      <button (click)="markAllAsRead()">Mark All Read</button>
      <button (click)="viewAllNotifications()">View All</button>
    </div>
  </div>
</div>
```

### **2. Notification Badge CSS**
```css
.notification-bell {
  position: relative;
  cursor: pointer;
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff4444;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 350px;
  max-height: 400px;
  overflow-y: auto;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 1000;
}

.notification-item {
  padding: 12px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.notification-item:hover {
  background: #f8f9fa;
}

.notification-title {
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.notification-message {
  color: #666;
  font-size: 14px;
  margin-bottom: 4px;
}

.notification-time {
  color: #999;
  font-size: 12px;
}
```

---

## **📱 Admin Dashboard Changes**

### **3. Notification Panel (Sidebar)**
```html
<!-- Add this to your admin dashboard sidebar -->
<div class="notification-panel">
  <h3>Recent Notifications</h3>
  <div class="notification-list">
    <div class="notification-card" *ngFor="let notification of recentNotifications">
      <div class="notification-header">
        <span class="notification-type" [class]="'type-' + notification.type">
          {{notification.type}}
        </span>
        <span class="notification-time">{{notification.createdAt | timeAgo}}</span>
      </div>
      <div class="notification-content">
        <h4>{{notification.title}}</h4>
        <p>{{notification.message}}</p>
        <div class="notification-details" *ngIf="notification.orderId">
          <span>Order #{{notification.orderId}}</span>
          <span>{{notification.customerName}}</span>
        </div>
      </div>
      <div class="notification-actions">
        <button (click)="viewOrder(notification.orderId)" *ngIf="notification.orderId">
          View Order
        </button>
        <button (click)="markAsRead(notification.id)" *ngIf="!notification.isRead">
          Mark Read
        </button>
      </div>
    </div>
  </div>
</div>
```

### **4. Dashboard Stats Card**
```html
<!-- Add this to your admin dashboard stats -->
<div class="stats-card notification-stats">
  <div class="stats-icon">
    <i class="fas fa-bell"></i>
  </div>
  <div class="stats-content">
    <h3>{{unreadCount}}</h3>
    <p>Unread Notifications</p>
  </div>
  <div class="stats-action">
    <button (click)="viewAllNotifications()">View All</button>
  </div>
</div>
```

---

## **🔧 TypeScript Services**

### **5. Notification Service (Angular)**
```typescript
// notification.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';

export interface AdminNotification {
  id: number;
  title: string;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
  orderId?: number;
  customerName?: string;
  customerMobile?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = 'http://localhost:8081/api/admin/notifications';
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private notificationsSubject = new BehaviorSubject<AdminNotification[]>([]);

  public unreadCount$ = this.unreadCountSubject.asObservable();
  public notifications$ = this.notificationsSubject.asObservable();

  constructor(private http: HttpClient) {
    this.startPolling();
  }

  // Get unread notifications
  getUnreadNotifications(): Observable<AdminNotification[]> {
    return this.http.get<AdminNotification[]>(`${this.apiUrl}`);
  }

  // Get recent notifications
  getRecentNotifications(): Observable<AdminNotification[]> {
    return this.http.get<AdminNotification[]>(`${this.apiUrl}/recent`);
  }

  // Get unread count
  getUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/count`);
  }

  // Mark notification as read
  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/read`, {});
  }

  // Mark all as read
  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {});
  }

  // Test email configuration
  testEmail(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/test-email`, {});
  }

  // Start polling for new notifications
  private startPolling() {
    timer(0, 30000).pipe( // Poll every 30 seconds
      switchMap(() => this.getUnreadCount())
    ).subscribe(count => {
      this.unreadCountSubject.next(count);
    });
  }

  // Refresh notifications
  refreshNotifications() {
    this.getUnreadNotifications().subscribe(notifications => {
      this.notificationsSubject.next(notifications);
    });
  }
}
```

### **6. Admin Component Integration**
```typescript
// admin-dashboard.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { NotificationService, AdminNotification } from './notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  unreadCount = 0;
  notifications: AdminNotification[] = [];
  recentNotifications: AdminNotification[] = [];
  showNotifications = false;
  private subscriptions = new Subscription();

  constructor(private notificationService: NotificationService) {}

  ngOnInit() {
    this.loadNotifications();
    this.subscribeToNotifications();
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }

  loadNotifications() {
    this.subscriptions.add(
      this.notificationService.unreadCount$.subscribe(count => {
        this.unreadCount = count;
      })
    );

    this.subscriptions.add(
      this.notificationService.getUnreadNotifications().subscribe(notifications => {
        this.notifications = notifications;
      })
    );

    this.subscriptions.add(
      this.notificationService.getRecentNotifications().subscribe(notifications => {
        this.recentNotifications = notifications;
      })
    );
  }

  subscribeToNotifications() {
    this.subscriptions.add(
      this.notificationService.notifications$.subscribe(notifications => {
        this.notifications = notifications;
      })
    );
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
  }

  markAsRead(id: number) {
    this.notificationService.markAsRead(id).subscribe(() => {
      this.notificationService.refreshNotifications();
    });
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notificationService.refreshNotifications();
    });
  }

  viewOrder(orderId: number) {
    // Navigate to order details
    console.log('View order:', orderId);
  }

  viewAllNotifications() {
    // Navigate to notifications page
    console.log('View all notifications');
  }

  testEmail() {
    this.notificationService.testEmail().subscribe(
      response => {
        console.log('Test email sent:', response);
        alert('Test email sent successfully!');
      },
      error => {
        console.error('Failed to send test email:', error);
        alert('Failed to send test email');
      }
    );
  }
}
```

---

## **🎨 CSS Styling**

### **7. Notification Styles**
```css
/* notification-styles.css */
.notification-panel {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 20px;
  margin: 20px 0;
}

.notification-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  background: white;
  transition: all 0.3s ease;
}

.notification-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notification-type {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  text-transform: uppercase;
}

.type-ORDER_PLACED {
  background: #e3f2fd;
  color: #1976d2;
}

.type-ORDER_STATUS_CHANGED {
  background: #fff3e0;
  color: #f57c00;
}

.type-SYSTEM {
  background: #f3e5f5;
  color: #7b1fa2;
}

.notification-time {
  color: #999;
  font-size: 12px;
}

.notification-content h4 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 16px;
}

.notification-content p {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.4;
}

.notification-details {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.notification-actions {
  display: flex;
  gap: 8px;
}

.notification-actions button {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.notification-actions button:first-child {
  background: #1976d2;
  color: white;
}

.notification-actions button:last-child {
  background: #f5f5f5;
  color: #666;
}

.notification-actions button:hover {
  opacity: 0.8;
}

/* Stats card styling */
.notification-stats {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats-icon {
  font-size: 24px;
  opacity: 0.8;
}

.stats-content h3 {
  margin: 0;
  font-size: 32px;
  font-weight: bold;
}

.stats-content p {
  margin: 4px 0 0 0;
  opacity: 0.8;
}

.stats-action button {
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.3);
  color: white;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.stats-action button:hover {
  background: rgba(255,255,255,0.3);
}
```

---

## **📋 Implementation Checklist**

### **✅ Frontend Changes Required:**

1. **Add Notification Bell to Header**
   - [ ] Notification bell icon with badge
   - [ ] Dropdown for recent notifications
   - [ ] Click handlers for interactions

2. **Create Notification Service**
   - [ ] HTTP service for API calls
   - [ ] Real-time polling (30-second intervals)
   - [ ] Observable streams for reactive updates

3. **Add Notification Panel to Dashboard**
   - [ ] Recent notifications list
   - [ ] Mark as read functionality
   - [ ] Order navigation links

4. **Add Stats Card**
   - [ ] Unread count display
   - [ ] Quick access to notifications

5. **Styling and Animations**
   - [ ] CSS for notification components
   - [ ] Hover effects and transitions
   - [ ] Responsive design

6. **Error Handling**
   - [ ] Network error handling
   - [ ] Loading states
   - [ ] User feedback messages

---

## **🚀 Quick Start**

### **1. Add to your main admin component:**
```html
<!-- In your admin header -->
<div class="notification-bell" (click)="toggleNotifications()">
  <i class="fas fa-bell"></i>
  <span class="notification-badge" *ngIf="unreadCount > 0">{{unreadCount}}</span>
</div>
```

### **2. Import the service:**
```typescript
// In your admin component
constructor(private notificationService: NotificationService) {}
```

### **3. Subscribe to updates:**
```typescript
ngOnInit() {
  this.notificationService.unreadCount$.subscribe(count => {
    this.unreadCount = count;
  });
}
```

---

## **🎯 Key Features**

- ✅ **Real-time updates** (30-second polling)
- ✅ **Unread count badge**
- ✅ **Notification dropdown**
- ✅ **Mark as read functionality**
- ✅ **Order navigation**
- ✅ **Responsive design**
- ✅ **Error handling**

**Your admin notification system is now ready for UI integration!** 🎉 