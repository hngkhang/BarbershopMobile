# Art Barbershop Design System

## Design Direction

Art Barbershop should look like a modern barber booking app.

Keywords:

- clean
- premium
- warm
- modern
- easy booking
- service-focused
- customer-friendly

The UI should not look like a generic student demo app.

## Color Palette

Recommended colors:

```xml
<color name="color_bg">#FAF7F2</color>
<color name="color_surface">#FFFFFF</color>
<color name="color_primary">#1F1F1F</color>
<color name="color_primary_dark">#111111</color>
<color name="color_accent">#C89B3C</color>
<color name="color_accent_light">#F3E2B8</color>
<color name="color_text_primary">#1F1F1F</color>
<color name="color_text_secondary">#6F6A64</color>
<color name="color_border">#E8E0D8</color>
<color name="color_success">#2E7D32</color>
<color name="color_warning">#F9A825</color>
<color name="color_error">#C62828</color>

Spacing
Use 8dp spacing system.
<dimen name="space_4">4dp</dimen>
<dimen name="space_8">8dp</dimen>
<dimen name="space_12">12dp</dimen>
<dimen name="space_16">16dp</dimen>
<dimen name="space_20">20dp</dimen>
<dimen name="space_24">24dp</dimen>
<dimen name="space_32">32dp</dimen>

Corner Radius
<dimen name="radius_small">8dp</dimen>
<dimen name="radius_medium">12dp</dimen>
<dimen name="radius_large">20dp</dimen>
Typography

Suggested usage:

Screen title: 24sp, bold
Section title: 18sp, semibold
Card title: 16sp, semibold
Body: 14sp
Caption: 12sp
Component Style
Service Card

Should include:

Service image/icon
Service name
Price
Duration
Short description
Book/select action
Barber Card

Should include:

Barber avatar
Name
Rating
Experience
Specialty
Availability status
Appointment Card

Should include:

Date and time
Service name
Barber name
Appointment status
Payment status
AI Chat Bubble

User bubble:

Align right
Primary color background
White text

AI bubble:

Align left
White or light beige background
Dark text
Optional suggestion chips
UI States

Every data screen should support:

Loading state
Empty state
Error state
Content state

Examples:

ServiceListActivity: no services found
BarberListActivity: no barber available
AppointmentActivity: no appointment yet
AIChatBookingActivity: AI is processing
```
