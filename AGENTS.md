# Art Barbershop - Codex Instructions

## Project Context

This is a native Android mobile application for a barbershop booking system.

Project name: Art Barbershop  
Language: Java  
UI: XML Layouts with Android Views  
Database: Firebase + SQLite

Main features:

- User register, login, edit profile
- View barber services
- View barber list, experience, rating, and working schedule
- Book appointment manually
- AI chat booking
- AI service/barber/time suggestion
- Appointment list and appointment detail
- Appointment reminder notification
- Barber review
- QR payment
- Firebase/SQLite synchronization

## Important Constraint

Do not migrate this project to Kotlin or Jetpack Compose.

Use:

- Java for Activity, Adapter, Service, BroadcastReceiver, DAO/helper classes
- XML for layouts
- Firebase for online data
- SQLite for local/offline data
- Material Components for Android when suitable

## UI Goal

The UI should look like a real modern barbershop mobile app, not an AI-generated demo.

The design should be:

- Clean
- Bright
- Modern
- Premium but not too flashy
- Suitable for haircut, grooming, salon, booking, and customer service

## Visual Style

Use a modern light theme.

Recommended style:

- Background: warm off-white or very light gray
- Primary color: dark charcoal, deep brown, or black
- Accent color: gold, amber, or warm beige
- Cards: soft rounded corners, subtle shadow
- Buttons: clear primary action
- Text: strong hierarchy between title, subtitle, body, and caption
- Images: barber/service images should be rounded and consistent

Avoid:

- Random gradients
- Neon colors
- Too many large cards
- Fake dashboard analytics
- Overly generic AI layout
- Web landing-page style
- Hardcoded colors and dimensions directly in XML

## Android UI Rules

- Use XML layouts.
- Prefer ConstraintLayout for screen layouts.
- Prefer RecyclerView for service list, barber list, appointments, and reviews.
- Prefer MaterialCardView for service cards, barber cards, and appointment cards.
- Prefer TextInputLayout for login/register/profile forms.
- Prefer BottomNavigationView in MainActivity for main app sections.
- Keep touch targets at least 48dp.
- Add contentDescription for important icons and images.
- Use tools attributes for Android Studio preview.

## Resource Rules

Do not hardcode repeated colors, dimensions, or text styles.

Use:

- `res/values/colors.xml`
- `res/values/dimens.xml`
- `res/values/styles.xml`
- `res/values/themes.xml`
- `res/drawable/` for reusable backgrounds
- `res/drawable/` for button/card shapes if needed

## Architecture Rules

Preserve existing business logic.

Do not change:

- Firebase collection structure unless requested
- SQLite schema unless requested
- Existing Activity navigation unless needed
- Existing view IDs if Java code references them
- Existing booking/payment/AI logic unless the task asks for it

When editing UI:

1. Find the Java Activity.
2. Find the related XML layout.
3. Check existing IDs.
4. Improve UI while keeping logic stable.
5. Update resources properly.
6. Explain changed files.

## Screen-Specific UI Direction

### SplashActivity

- Simple logo
- App name
- Short tagline
- Loading indicator
- No crowded UI

### LoginActivity / RegisterActivity

- Clean authentication screen
- App branding at top
- TextInputLayout fields
- Primary button
- Secondary link for register/login
- Error message area

### MainActivity

- Use BottomNavigationView if the app has Home, Appointments, AI Booking, Profile
- Keep navigation simple

### HomeActivity

- Show welcome text
- Featured services
- Recommended barber
- Quick booking button
- AI booking entry point
- Upcoming appointment card if available

### ServiceListActivity

- RecyclerView grid or vertical list
- Each service card should show:
  - service name
  - short description
  - price
  - estimated duration
  - image/icon
  - select/book button

### BarberListActivity

- RecyclerView list
- Each barber card should show:
  - avatar/image
  - name
  - experience
  - rating
  - specialties
  - available status
  - view schedule/book button

### BookingActivity

- Step-by-step booking layout:
  1. Select service
  2. Select barber
  3. Select date
  4. Select time
  5. Confirm
- Avoid putting too much information on one screen

### AIChatBookingActivity

- Chat-style UI
- User message bubble
- AI message bubble
- Suggestion chips for service/time/barber
- Bottom input field
- Send button
- Loading state while AI is processing

### BookingConfirmActivity

- Confirmation summary card
- Show selected service, barber, date, time, price
- Allow user to edit before confirming
- Primary action: Confirm and continue to payment

### PaymentActivity

- Show QR code clearly
- Show amount
- Show transfer content
- Show payment status
- Primary action: I have paid / Check payment status

### AppointmentActivity

- List of appointments
- Tabs or filters:
  - Upcoming
  - Completed
  - Cancelled
- Appointment cards should clearly show date/time/service/barber/status

### AppointmentDetailActivity

- Full appointment information
- Payment status
- Reminder status
- Cancel/reschedule action if applicable

### ReviewActivity

- Rating bar
- Comment box
- Submit button
- Barber/service summary

### ProfileActivity

- User avatar
- Name, phone, email
- Edit profile action
- Logout action

## Services and Receivers

Do not turn Android Service classes into UI classes.

For:

- AIBookingService
- PaymentService
- ReminderService
- SyncService
- ReminderReceiver
- BootReceiver
- NetworkReceiver

Only modify them when the requested task is related to logic, notification, payment, sync, or AI booking.

## Final Response Requirement

After modifying code, always report:

1. Files changed
2. What changed in each file
3. Whether Java logic was preserved
4. Whether UI resources were added or changed
5. How to manually test the screen
