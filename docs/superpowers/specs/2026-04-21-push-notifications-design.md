# Push Notifications Design — Aura App

**Date:** 2026-04-21

---

## Overview

Add push notifications to the Aura todo app via two mechanisms:
- **External:** Firebase Cloud Messaging (FCM) via Cloud Functions for test notifications
- **Internal:** WorkManager for scheduled notifications (daily summary + due date reminders)

---

## Notification Types

### 1. Daily Summary
- Fires once per day at a user-configured time (default 8:00 AM)
- Shows: "You have N incomplete tasks" or "All tasks complete! 🎉"
- User-configurable time via inline picker in TodoScreen

### 2. Due Date Reminder
- Fires at the user's configured notification time on the todo's due date
- Shows: "Task [name] is due today"
- Scheduled when todo is created or due date is set/modified

### 3. External Test Notification
- Triggered via Cloud Functions HTTP endpoint
- Purpose: Test that FCM is properly wired up
- Can be tested via Firebase Console or `firebase functions:shell`

---

## Model Change

### Todo.kt
Add `dueDate: Long?` field (epoch millis, nullable):

```kotlin
data class Todo(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null  // epoch millis, nullable
)
```

---

## Components

### Data Layer
| File | Responsibility |
|------|----------------|
| `NotificationPreferences` | DataStore-backed preferences for notification time (hour, minute) and enabled state |

### Domain Layer
No changes — notifications are a presentation/infrastructure concern.

### Presentation Layer
| File | Responsibility |
|------|----------------|
| `NotificationHelper` | Creates notification channels, builds and shows notifications |
| `DailySummaryWorker` | Periodic WorkManager worker, fires at configured time |
| `DueDateNotificationWorker` | One-time worker, scheduled when todo is created/edited |
| `NotificationViewModel` | Manages notification preference state |

### DI
| File | Responsibility |
|------|----------------|
| `NotificationModule` | Registers NotificationHelper, NotificationPreferences, NotificationViewModel |

---

## Notification Channels

| Channel ID | Name | Importance |
|------------|------|------------|
| `daily_summary` | Daily Summary | Default |
| `due_date_reminder` | Due Date Reminders | High |

---

## WorkManager Scheduling

### Daily Summary
- `PeriodicWorkRequestBuilder<DailySummaryWorker>` with `setInitialDelay` calculated to fire at user's configured time
- Re-scheduled on app start and when user changes the time
- Uses `ExistingPeriodicWorkPolicy.CANCEL_AND_RECREATE` to update schedule

### Due Date Reminder
- `OneTimeWorkRequestBuilder<DueDateNotificationWorker>` with input data: todo title and todo ID
- Scheduled immediately when todo is created with a due date or when due date is set
- Canceled and rescheduled when due date changes or todo is deleted

---

## Data Flow

```
Todo created with dueDate
  → WorkManager schedules DueDateNotificationWorker
  → At due date, worker fires → NotificationHelper.showDueDateNotification()

App start
  → WorkManager schedules DailySummaryWorker at configured time
  → DailySummaryWorker fires → checks incomplete todos → shows summary notification

Time preference changed
  → Cancel existing DailySummaryWorker
  → Schedule new one at updated time

Cloud Function HTTP trigger
  → FCM sends push to app → FirebaseMessagingService handles → shows test notification
```

---

## UI

### TodoScreen Additions
- **Notification settings row** below todo list:
  - Toggle: "Daily reminder"
  - Time picker: "08:00 AM" (opens system time picker)
- Only visible/functional on Android (iOS notification scheduling via different mechanism — out of scope for v1)

---

## Cloud Functions

### sendTestNotification (HTTP)
```
POST /sendTestNotification
Body: { "title": "string", "body": "string" }
```

Sends FCM downstream message to the `test-notifications` topic. App subscribes to this topic on startup.

---

## Dependencies (libs.versions.toml additions)

```toml
firebase-messaging-ktx = "24.0.0"  # or latest via bom
workmanager-ktx = "2.9.0"
datastore-preferences = "1.1.0"
```

---

## Out of Scope
- iOS push notification setup (different mechanism, requires APNs)
- Background execution on iOS
- Notification action buttons (mark complete from notification)
- Notification history/log screen

---

## Success Criteria
- [ ] User can set daily notification time from TodoScreen
- [ ] Daily summary fires at configured time showing incomplete task count
- [ ] Todo with due date triggers reminder at configured time on due date
- [ ] Cloud Function can be triggered to send test notification to app
- [ ] All notifications respect Android notification channel settings