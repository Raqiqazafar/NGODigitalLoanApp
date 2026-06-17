# 📱 NGO Digital Loan App — Ehsan Digital

> A complete Android application for managing microfinance loan operations for NGOs — from loan application to repayment tracking, all in one place.

---

## 🌟 Overview

**NGO Digital Loan App** is an Android-based loan management system built for *Ehsan Digital* (an NGO). It digitizes the entire loan lifecycle — users can apply for loans, track approvals, pay installments, and view repayment history. Admins can manage users, review loan requests, approve or reject applications, and monitor live financial statistics — all powered by Firebase.

---

## ✨ Features

### 👤 User Side
- Register & Login with Email/Password or Google Sign-In
- Forgot Password via email reset link
- Home Dashboard with live balance & outstanding debt
- Apply for loans by category (Education, Business, Medical, etc.)
- Pay installments and track remaining balance
- View complete repayment history
- View loan details and status (Pending / Approved / Rejected)
- Personal profile management

### 🛡️ Admin Side
- Secure admin dashboard with live statistics
  - Total active users
  - Pending loan requests count
  - Total disbursed amount
  - Recovery percentage (live calculated)
- Review and approve / reject loan requests
- Set custom approved amount via slider
- Manage all users (edit, block, archive)
- Add new borrowers manually
- Reports & analytics screen

---

## 🏗️ Project Architecture

```
NGODigitalLoanApp/
├── app/src/main/java/com/example/ngodigitalloan/
│   ├── SplashActivity.java
│   ├── MainActivity.java
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── ForgotPasswordActivity.java
│   ├── HomeActivity.java              ← User Dashboard (Real-time)
│   ├── ApplyLoanActivity.java         ← Loan Application Form
│   ├── PayInstallmentActivity.java    ← Installment Payment
│   ├── RepaymentHistoryActivity.java  ← Payment History
│   ├── LoanDetailActivity.java        ← Single Loan Details
│   ├── LoansListActivity.java         ← All Loans List
│   ├── ProfileActivity.java           ← User Profile
│   ├── AdminDashboardActivity.java    ← Admin Home (Live Stats)
│   ├── LoanRequestsActivity.java      ← All Pending Requests
│   ├── LoanReviewActivity.java        ← Approve / Reject Loan
│   ├── ManageUsersActivity.java       ← User Management
│   ├── AddBorrowerActivity.java       ← Add New User
│   ├── ReportsActivity.java           ← Analytics & Reports
│   └── AdminSettingsActivity.java
├── app/src/main/res/
│   ├── layout/                        ← All XML Layouts
│   └── values/                        ← Colors, Strings, Themes
└── google-services.json               ← Firebase Config
```

---

## 🗄️ Firebase Database Structure

### `Users` Collection
```
Users/
  {userID}/
    name          : "Ali Ahmed"
    email         : "ali@gmail.com"
    cnic          : "3520199999999"
    phone         : "03001234567"
    role          : "user"           // "user" or "admin"
    accountStatus : "Active"         // "Active", "Blocked", "Archived"
    balance       : "Rs 50000"
```

### `Loans` Collection
```
Loans/
  {autoID}/
    userId          : "abc123uid"
    loanType        : "Education Loan"
    requestedAmount : "100000"
    approvedAmount  : "80000"
    paidAmount      : "20000"
    repaymentPlan   : "Short Term (6 Months)"
    status          : "Pending"      // "Pending", "Approved", "Rejected"
    adminRemarks    : ""
    timestamp       : ServerTimestamp
```

---

## 🔐 Authentication Flow

```
User Opens App
      ↓
LoginActivity
      ↓
Firebase Auth (Email/Password  OR  Google Sign-In)
      ↓
checkUserRoleAndStatus()  ← Firestore se role check karo
      ↓
   role == "admin"  →  AdminDashboardActivity
   role == "user"   →  HomeActivity
   status == "Blocked" / "Archived"  →  Sign Out + Error Message
```

---

## 🔄 CRUD Operations

| Operation | Where Used | Firestore Method |
|-----------|-----------|-----------------|
| **Create** | Register User, Apply Loan | `collection.set()` / `collection.add()` |
| **Read** | Dashboard, Loan History | `document.get()` / `addSnapshotListener()` |
| **Update** | Approve Loan, Pay Installment, Block User | `document.update()` |
| **Delete** | Archive User (Soft Delete) | `document.update("accountStatus", "Archived")` |

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java** | Primary programming language |
| **Android SDK** | Mobile app framework |
| **Firebase Authentication** | User login (Email + Google) |
| **Firebase Firestore** | NoSQL real-time database |
| **Material Design 3** | UI components & theming |
| **Gradle (Kotlin DSL)** | Build system |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- Java 8 or higher
- A Firebase project

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/NGODigitalLoanApp.git
   cd NGODigitalLoanApp
   ```

2. **Firebase Setup**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project
   - Add an Android app with package name: `com.example.ngodigitalloan`
   - Download `google-services.json` and place it in the `app/` folder
   - Enable **Authentication** → Email/Password + Google Sign-In
   - Enable **Firestore Database** → Start in test mode

3. **Open in Android Studio**
   - File → Open → Select the project folder
   - Wait for Gradle sync to complete

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click ▶ Run

---

## 📸 App Screens

| Screen | Description |
|--------|-------------|
| Login / Register | Email + Google Sign-In with validation |
| Home Dashboard | Live balance, debt, quick action buttons |
| Apply Loan | Category selection, amount & repayment plan |
| Pay Installment | Select loan, enter amount, wallet deduction |
| Admin Dashboard | Live stats: users, disbursed, recovery % |
| Loan Review | Approve/Reject with slider for custom amount |
| Manage Users | Block, archive, edit user details |

---

## 📦 Dependencies

```gradle
// Firebase
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.android.gms:play-services-auth'

// Material Design
implementation 'com.google.android.material:material:1.12.0'

// AndroidX
implementation 'androidx.appcompat:appcompat'
implementation 'androidx.constraintlayout:constraintlayout'
```

---

## 👨‍💻 Developer

Built as a 6th Semester project for BS Computer Science.  
**Organization:** Ehsan Digital (NGO)  
**Platform:** Android (Java)  
**Backend:** Firebase (Serverless)

---

## 📄 License

This project is for academic/educational purposes.

