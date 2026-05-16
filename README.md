# Random Skill Gen-ZRator

Daily quest gamification app built with Android Java, Firebase Authentication, Firestore sync, Room local storage, ViewModel + LiveData, WorkManager reminders, XP, level, streak, achievements, history, stats, dark mode, and custom skills.

## Architecture

- `presentation`: Activity, Fragment, ViewModel, and Adapter classes in the main package.
- `database`: Room entities, DAO, database, and local repository.
- `data`: Firebase sync boundary.
- `domain`: small business-rule helpers for XP, streak, timer, tier labels, and validation.

The app is offline-first: Room is the source used by UI, and Firestore sync is best-effort. If cloud reads fail, local data should still load.

## Custom Challenge Rules

- Users can create up to 30 custom challenges per account.
- Custom challenge titles are normalized, trimmed, and checked case-insensitively to avoid duplicates.
- Difficulty controls timer and XP:
  - Easy: 1 minute, category XP minus 20, minimum 20 XP.
  - Medium: 3 minutes, category XP.
  - Hard: 5 minutes, category XP plus 30.
- Cloud-restored custom challenge values are sanitized before being written to Room.

## Firebase Security Notes

For a public repository, avoid committing a real `app/google-services.json`. Keep it local or provide a sanitized sample, and restrict Firebase API keys in Google Cloud.

Recommended Firestore Rules shape:

```text
match /users/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
  match /{document=**} {
    allow read, write: if request.auth != null && request.auth.uid == userId;
  }
}
```

Do not store auth tokens, raw evidence photos/videos, or sensitive profile data in Firestore.
Keep Firestore documents scoped under `/users/{uid}` and validate client-side data again on the server/rules side for production releases.
