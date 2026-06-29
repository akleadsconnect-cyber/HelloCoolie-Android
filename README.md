# HelloCoolie Android App 📱
### *"Your Porter, Just a Hello Away!"*
**Kotlin · MVVM · Hilt DI · Retrofit · Socket.IO**

---

## 🏗️ Architecture

```
MVVM + Clean Architecture
├── data/
│   ├── model/        — All data classes (User, Porter, Booking, etc.)
│   ├── api/          — Retrofit API interface + Network module (Hilt)
│   └── repository/   — AuthRepository, BookingRepository, PorterRepository
├── ui/
│   ├── splash/       — SplashActivity (auto-routes based on session)
│   ├── auth/         — AuthActivity + Login/Register fragments (User + Porter)
│   ├── user/         — UserMainActivity + Home/Book/Bookings/Profile fragments
│   └── porter/       — PorterMainActivity + Dashboard/Earnings/Wallet/Profile
├── services/
│   ├── SocketManager          — Socket.IO real-time events
│   └── HelloCoolieFirebaseService — FCM push notifications
└── utils/
    └── TokenManager           — DataStore auth token + session
```

---

## 👥 Two App Roles

### 👤 Passenger (User)
| Screen | Features |
|--------|---------|
| Login | Mobile + password, forgot via DOB |
| Register | Name, phone, password, DOB |
| Home | Book coolie button, SOS |
| Book Coolie | PNR, bags (count + weight), drop location, senior/women toggle, fare preview, online/cash |
| Searching | Live animation while finding porter |
| Booking Active | Show OTP to porter, live status |
| My Bookings | History with status, cancel, rate |
| Profile | Edit profile, language, logout |

### 🔴 Porter
| Screen | Features |
|--------|---------|
| Login | Mobile + password, forgot via Aadhaar |
| Register | Full 12-field registration (shift, badge, station, address, emergency contact, blood group, UPI) |
| Dashboard | Online/offline toggle, accept/reject alerts, OTP entry, complete job, SOS |
| Earnings | Today/Week/Month/All-time, best day, 7-day bar chart (MPAndroidChart) |
| Wallet | Balance, transaction history, instant withdrawal to UPI |
| Profile | Stats, language switcher (Hindi/English), logout |

---

## 🔔 Real-time Features

### Uber-like Booking Alert (Porter)
```
1. Loud vibration + audio
2. Full-screen dialog shows:
   - Station, train no, coach
   - Bag count + weight
   - DROP location
   - YOUR earnings (not total)
   - 30-second countdown
   - ACCEPT (green) / REJECT (red) buttons
   - Senior citizen / Woman alone tags
3. If 30s expires → goes to next porter automatically
```

### Socket.IO Events
| Event | Who gets it | What happens |
|-------|------------|-------------|
| `new_booking_request` | Porter | Vibrate + dialog |
| `porter_assigned` | User | "Porter found! Show OTP" |
| `job_started` | User | Contact revealed |
| `job_completed` | User | Rate porter prompt |
| `trolley_offer` | User | Accept/decline dialog |
| `booking_expired` | User | Full refund message |

---

## 🎨 Brand Colors
```
Hello (Orange) = #F47920  ← Primary
Coolie (Blue)  = #1B75BB  ← Secondary
Green          = #16A34A  ← Success
Red            = #DC2626  ← Error/Danger
Amber          = #D97706  ← Warning
```

---

## 🔧 Setup

### 1. Add to `local.properties`:
```
MAPS_API_KEY=your_google_maps_api_key
```

### 2. Add `google-services.json` to `/app/`

### 3. Build variants:
```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

### 4. API Configuration:
Edit `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "BASE_URL", "\"https://hellocoolie.onrender.com/api/\"")
buildConfigField("String", "RAZORPAY_KEY", "\"rzp_live_your_key\"")
```

---

## 📦 Tech Stack

| Library | Purpose |
|---------|---------|
| Retrofit 2.11 | REST API calls |
| OkHttp 4.12 | HTTP client + logging |
| Gson | JSON parsing |
| Hilt 2.51 | Dependency injection |
| Socket.IO 2.1 | Real-time notifications |
| DataStore | Auth token storage |
| Room | Local booking cache |
| Coroutines | Async operations |
| Navigation | Fragment routing |
| ViewBinding | View access |
| Razorpay 1.6.40 | Payments |
| Lottie 6.4 | Loading animations |
| MPAndroidChart | Earnings bar chart |
| Glide 4.16 | Image loading |

---

## 📁 Layouts Needed (to add in Android Studio)

```
res/layout/
├── activity/
│   ├── activity_auth.xml         — ViewPager2 + TabLayout
│   ├── activity_user_main.xml    — BottomNav + fragment container
│   └── activity_porter_main.xml  — BottomNav + fragment container
├── fragment/
│   ├── fragment_user_login.xml
│   ├── fragment_porter_login.xml
│   ├── fragment_user_register.xml
│   ├── fragment_porter_register.xml
│   ├── fragment_forgot_password.xml
│   ├── fragment_user_home.xml
│   ├── fragment_book_coolie.xml
│   ├── fragment_booking_searching.xml
│   ├── fragment_user_bookings.xml
│   ├── fragment_user_profile.xml
│   ├── fragment_porter_dashboard.xml
│   ├── fragment_porter_earnings.xml
│   ├── fragment_porter_wallet.xml
│   └── fragment_porter_profile.xml
└── item/
    ├── item_booking.xml
    └── item_transaction.xml
```

*HelloCoolie | Akshay Rai | 2026 | "Your Porter, Just a Hello Away!"*
