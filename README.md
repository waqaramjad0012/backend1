# 🎂 Birthday Card Link Generator

Full-stack birthday card system: Android app → Node.js API → MongoDB → Shareable HTML card page.

---

## Project Structure

```
birthday-project/
├── backend/              ← Node.js + Express API (deploy to Vercel)
│   ├── index.js
│   ├── models/Card.js
│   ├── package.json
│   ├── vercel.json
│   └── .env.example
├── frontend/             ← Plain HTML card page (deploy to Vercel)
│   └── card.html
└── BirthdayCardApi.kt    ← Android Kotlin integration code
```

---

## Step 1 — MongoDB Atlas Setup

1. Go to https://cloud.mongodb.com and create a free cluster
2. Create a database user (username + password)
3. Whitelist `0.0.0.0/0` in Network Access (for Vercel's dynamic IPs)
4. Copy your connection string:
   ```
   mongodb+srv://<user>:<pass>@cluster0.xxxxx.mongodb.net/birthdayapp
   ```

---

## Step 2 — Deploy Backend to Vercel

```bash
cd backend
npm install

# Install Vercel CLI globally
npm i -g vercel

# Deploy
vercel

# Set environment variables on Vercel dashboard or via CLI:
vercel env add MONGODB_URI
vercel env add FRONTEND_URL    # e.g. https://birthday-cards.vercel.app
```

Your API will be live at: `https://your-backend.vercel.app`

### API Endpoints

| Method | Endpoint          | Description              |
|--------|-------------------|--------------------------|
| GET    | `/`               | Health check             |
| POST   | `/api/cards`      | Create a new card        |
| GET    | `/api/cards/:slug`| Fetch card data by slug  |

**POST /api/cards — Request body:**
```json
{
  "recipientName": "Sarah",
  "age": 25,
  "message": "Wishing you all the happiness in the world!",
  "senderName": "Ali",
  "theme": "pink"
}
```

**Response:**
```json
{
  "success": true,
  "slug": "aB3kZ9mQ",
  "url": "https://your-frontend.vercel.app/card.html?id=aB3kZ9mQ"
}
```

**Themes:** `pink` | `blue` | `gold` | `green`

---

## Step 3 — Deploy Frontend to Vercel

1. In `card.html`, replace `API_BASE` with your actual backend URL:
   ```js
   const API_BASE = 'https://your-backend.vercel.app';
   ```

2. Deploy the `frontend/` folder:
   ```bash
   cd frontend
   vercel
   ```

3. Copy the deployed URL and set it as `FRONTEND_URL` in your backend env.

---

## Step 4 — Android Integration

1. Add dependencies to `build.gradle`:
   ```groovy
   implementation 'com.squareup.retrofit2:retrofit:2.9.0'
   implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
   implementation 'com.squareup.okhttp3:okhttp:4.12.0'
   ```

2. Copy `BirthdayCardApi.kt` into your project and replace `BASE_URL`.

3. Add internet permission to `AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

4. Use `BirthdayCardViewModel` in your fragment/activity — see the usage
   example at the bottom of `BirthdayCardApi.kt`.

---

## Flow Summary

```
Android App
  └─ User fills: name, age, message, theme
  └─ POST /api/cards  →  Backend
                            └─ Saves to MongoDB
                            └─ Returns shareable URL
  └─ Android shows Share Sheet with URL

Recipient opens URL in browser
  └─ card.html loads
  └─ Fetches GET /api/cards/:slug
  └─ Renders animated birthday card
  └─ User taps candles → confetti + message reveal
```

---

## Notes

- Cards auto-expire after **90 days** (MongoDB TTL index)
- Slug is 8 characters (nanoid) — collision-resistant for millions of cards
- CORS is locked to your frontend URL in production
- No auth needed — slugs are unguessable by design
