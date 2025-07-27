# 📧 Gmail Setup Guide for Admin Notifications

## **Overview**
This guide will help you set up Gmail to send admin notifications when orders are placed.

---

## **🔧 Step-by-Step Setup**

### **Step 1: Enable 2-Factor Authentication**
1. Go to [Google Account Settings](https://myaccount.google.com/)
2. Click on **Security**
3. Enable **2-Step Verification** if not already enabled
4. This is required to generate App Passwords

### **Step 2: Generate App Password**
1. In Google Account Settings → **Security**
2. Find **2-Step Verification** and click **App passwords**
3. Select **Mail** as the app
4. Select **Other (Custom name)** as device
5. Enter name: `HandPick Admin Notifications`
6. Click **Generate**
7. **Copy the 16-character password** (e.g., `abcd efgh ijkl mnop`)

### **Step 3: Update Application Properties**
Replace `your-gmail-app-password` in `application.properties`:

```properties
# Admin Email Notifications Configuration
app.notifications.email.enabled=true
app.notifications.email.admin=mayurshete1331@gmail.com
app.notifications.email.smtp.host=smtp.gmail.com
app.notifications.email.smtp.port=587
app.notifications.email.username=mayurshete1331@gmail.com
app.notifications.email.password=abcd efgh ijkl mnop
```

**⚠️ Important:** 
- Use the App Password, NOT your regular Gmail password
- Remove spaces from the App Password when adding to properties

### **Step 4: Test the Configuration**
1. Restart your Spring Boot application
2. Place a test order
3. Check your email (mayurshete1331@gmail.com) for notifications

---

## **📧 Email Notification Format**

When an order is placed, you'll receive an email like this:

```
Subject: New Order #123 - HandPick

🔔 NEW ORDER ALERT!

Order ID: 123
Customer: John Doe (9876543210)
Amount: ₹500.00
Payment: COD

Please check your admin dashboard for details.
```

---

## **🔍 Troubleshooting**

### **Common Issues:**

#### **1. Authentication Failed**
```
Error: Authentication failed
```
**Solution:** 
- Make sure you're using App Password, not regular password
- Verify 2-Factor Authentication is enabled
- Check that the App Password is copied correctly

#### **2. Connection Timeout**
```
Error: Connection timeout
```
**Solution:**
- Check internet connection
- Verify SMTP settings are correct
- Try port 587 (TLS) or 465 (SSL)

#### **3. Email Not Sending**
```
Error: Email notifications disabled
```
**Solution:**
- Set `app.notifications.email.enabled=true`
- Verify all email properties are configured

### **Test Configuration:**
```bash
# Check if email service is working
curl -X POST http://localhost:8081/api/admin/test-email
```

---

## **🔒 Security Notes**

### **App Password Security:**
- App Passwords are specific to your application
- They can be revoked without affecting your main account
- Store them securely (not in version control)
- Use environment variables in production

### **Production Setup:**
```properties
# Use environment variables in production
app.notifications.email.password=${GMAIL_APP_PASSWORD}
```

---

## **✅ Verification Checklist**

- [ ] 2-Factor Authentication enabled
- [ ] App Password generated
- [ ] Application properties updated
- [ ] Application restarted
- [ ] Test order placed
- [ ] Email notification received

---

## **🎉 Success!**

Once configured, you'll receive instant email notifications for:
- ✅ New orders placed
- ✅ Order status changes
- ✅ System events

**Your admin notification system is now fully operational!** 🚀 