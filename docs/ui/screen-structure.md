# Art Barbershop Screen Structure

## Main Navigation

Recommended bottom navigation:

1. Home
2. Services
3. AI Booking
4. Appointments
5. Profile

If the current project uses separate Activities, keep the current Activity structure unless explicitly asked to refactor.

## Screen Map

### SplashActivity

Purpose:

- Check login state
- Navigate to LoginActivity or MainActivity

UI:

- Logo
- App name
- Loading indicator

### LoginActivity

Purpose:

- User login

UI:

- Email/phone field
- Password field
- Login button
- Register link

### RegisterActivity

Purpose:

- User registration

UI:

- Name
- Phone
- Email
- Password
- Confirm password
- Register button

### HomeActivity

Purpose:

- Main customer landing page

UI sections:

- Greeting
- Search or quick action
- Featured services
- Top barbers
- Upcoming appointment
- AI booking button

### ServiceListActivity

Purpose:

- Display barbershop services

UI:

- Category chips: Cut, Wash, Curl, Dye, Combo
- Service RecyclerView
- Service item card

### BarberListActivity

Purpose:

- Display barber list

UI:

- Barber RecyclerView
- Filter by rating/specialty/availability if available

### BookingActivity

Purpose:

- Manual booking flow

UI:

- Step indicator or separated sections
- Service selection
- Barber selection
- Date selection
- Time slot selection
- Continue button

### AIChatBookingActivity

Purpose:

- Let user type natural language booking request

Example user input:

- "Đặt lịch giúp tôi cắt tóc chiều thứ ba lúc 1 giờ"
- "Tư vấn cho tôi combo phù hợp với tóc nam ngắn"
- "Tôi muốn nhuộm tóc cuối tuần này"

UI:

- Chat messages
- Suggestion chips
- Input bar
- Send button
- Loading bubble

### BookingConfirmActivity

Purpose:

- Show AI-generated temporary appointment before user confirms

UI:

- Confirmation card
- Service
- Barber
- Date/time
- Price
- Edit button
- Confirm button

### PaymentActivity

Purpose:

- QR payment

UI:

- QR code
- Amount
- Transfer content
- Payment status
- Check status button

### AppointmentActivity

Purpose:

- Show appointments

UI:

- Tabs/filter by appointment status
- Appointment cards

### AppointmentDetailActivity

Purpose:

- Show full appointment detail

UI:

- Status
- Service
- Barber
- Date/time
- Payment
- Reminder
- Cancel/reschedule if applicable

### ReviewActivity

Purpose:

- User reviews barber

UI:

- Barber summary
- RatingBar
- Comment field
- Submit button

### ProfileActivity

Purpose:

- User profile

UI:

- Avatar
- Name
- Phone
- Email
- Edit profile
- Logout
