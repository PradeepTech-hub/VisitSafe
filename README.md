# 🛡️ VisitSafe - Smart Visitor Management System

🔗 Repository: https://github.com/PradeepTech-hub/VisitSafe.git

---

## 📌 Overview

**VisitSafe** is a smart Android application designed to digitize and secure visitor management in **residential apartments and gated communities**.

It replaces traditional manual entry systems with a **modern, role-based digital solution**, ensuring better security, faster access, and real-time monitoring.

---

## 🎯 Objective

The main goal of VisitSafe is to:

- Enhance **security and visitor tracking**
- Eliminate **manual register errors**
- Provide **real-time communication**
- Improve **efficiency for residents and security staff**

---

## 🚀 Key Features

### 👥 Multi-Role System

VisitSafe supports three main roles:

#### 🏠 Residents
- Create visitor invitations
- Generate QR codes for entry
- Receive real-time visitor notifications
- Manage family members

---

#### 🛡️ Security Personnel
- Scan QR codes for verification
- Validate visitor invite codes
- Record entry and exit logs
- Ensure secure access control

---

#### 🧑‍💼 Admin
- Manage apartments and residents
- Monitor all visitor activities
- Maintain system-wide control and logs

---

### 📲 QR Code-Based Entry

- Visitors receive QR codes from residents  
- Security scans QR codes for **instant verification**  
- Eliminates manual data entry  

---

### 🔐 Secure Authentication

- OTP-based login using **Firebase Authentication**
- Ensures **verified and secure user access**

---

### 🔔 Real-Time Notifications

- Residents receive instant alerts when visitors arrive  
- Powered by **Firebase Cloud Messaging (FCM)**  

---

### ☁️ Cloud Database

- Uses **Firebase Firestore**
- Real-time data synchronization
- Secure and scalable storage

---

### 👤 Profile Management

- Update personal details
- Role-based access control
- Easy user management

---

## 🛠️ Tech Stack

- **Language**: Java  
- **UI**: Android XML (View Binding)  
- **Backend**: Firebase  
  - Authentication (OTP Login)  
  - Firestore Database  
  - Cloud Messaging (FCM)  

---

## 📚 Libraries Used

- **ZXing** → QR Code generation & scanning  
- **Material Components** → Modern UI design  
- **Android Lifecycle (ViewModel & LiveData)** → Reactive programming  

---

## ⚙️ Installation & Setup

1. Clone the repository:

```bash
git clone https://github.com/PradeepTech-hub/VisitSafe.git




## ⚙️ Installation & Setup

1. Open the project in **Android Studio**

2. Add Firebase configuration:
   - Download `google-services.json`
   - Place it inside the `app/` folder
   - Enable the following services in Firebase Console:
     - Phone Authentication
     - Firestore Database
     - Cloud Messaging (FCM)

3. Sync Gradle

4. Run the app on:
   - Emulator OR
   - Physical device

---

## 🔒 Security Highlights

- OTP-based authentication  
- Role-based access control  
- QR-based secure entry  
- Real-time monitoring  

---

## 🌟 Future Enhancements

- Face recognition for visitors  
- Visitor analytics dashboard  
- Emergency alert system  
- Multi-language support  
- Offline functionality  

---

## 🤝 Contribution

Contributions are welcome!

You can contribute by:

- Improving UI/UX  
- Adding new features  
- Enhancing security  
- Optimizing performance  
