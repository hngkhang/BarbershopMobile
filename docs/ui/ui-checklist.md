# UI Review Checklist

Before finishing any UI task, check:

## Visual

- Is the screen visually consistent with Art Barbershop?
- Does it use the project color palette?
- Does it use consistent spacing?
- Is the visual hierarchy clear?
- Are primary and secondary actions easy to recognize?
- Does the UI avoid generic AI-style gradients and fake widgets?

## Android

- Are colors moved to colors.xml?
- Are dimensions moved to dimens.xml?
- Are repeated drawables reusable?
- Are existing view IDs preserved?
- Is the layout responsive?
- Are touch targets at least 48dp?
- Are contentDescription values added where needed?
- Are tools preview attributes used where useful?

## State

- Loading state exists
- Empty state exists
- Error state exists
- Content state exists

## Code Safety

- Existing Java logic is preserved
- Existing navigation is preserved
- Existing Firebase/SQLite logic is preserved
- No unrelated screen was rewritten
- No migration to Kotlin or Compose
