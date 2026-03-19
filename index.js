require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const { nanoid } = require('nanoid');
const Card = require('./models/Card');

const app = express();
const PORT = process.env.PORT || 3000;

// ─── Middleware ───────────────────────────────────────────────────────────────
app.use(express.json());
app.use(cors({
  origin: process.env.FRONTEND_URL || '*',
  methods: ['GET', 'POST'],
}));

// ─── MongoDB Connection ───────────────────────────────────────────────────────
mongoose.connect(process.env.MONGODB_URI)
  .then(() => console.log('✅ MongoDB connected'))
  .catch(err => {
    console.error('❌ MongoDB connection error:', err.message);
    process.exit(1);
  });

// ─── Routes ───────────────────────────────────────────────────────────────────

// Health check
app.get('/', (req, res) => {
  res.json({ status: 'ok', message: 'Birthday Card API is running 🎂' });
});

/**
 * POST /api/cards
 * Create a new birthday card and return the shareable slug
 *
 * Body: { recipientName, age, message, senderName?, theme? }
 * Returns: { slug, url }
 */
app.post('/api/cards', async (req, res) => {
  try {
    const { recipientName, age, message, senderName, theme } = req.body;

    // Basic validation
    if (!recipientName || !age || !message) {
      return res.status(400).json({
        error: 'recipientName, age, and message are required.',
      });
    }

    if (typeof age !== 'number' || age < 1 || age > 150) {
      return res.status(400).json({ error: 'age must be a number between 1 and 150.' });
    }

    if (message.length > 500) {
      return res.status(400).json({ error: 'message must be 500 characters or less.' });
    }

    // Generate a unique 8-char slug
    const slug = nanoid(8);

    const card = await Card.create({
      slug,
      recipientName: recipientName.trim(),
      age,
      message: message.trim(),
      senderName: senderName?.trim() || '',
      theme: theme || 'pink',
    });

    const cardUrl = `${process.env.FRONTEND_URL}/card.html?id=${card.slug}`;

    return res.status(201).json({
      success: true,
      slug: card.slug,
      url: cardUrl,
    });
  } catch (err) {
    console.error('POST /api/cards error:', err);
    return res.status(500).json({ error: 'Internal server error.' });
  }
});

/**
 * GET /api/cards/:slug
 * Fetch card data by slug (used by the frontend card page)
 *
 * Returns: { recipientName, age, message, senderName, theme }
 */
app.get('/api/cards/:slug', async (req, res) => {
  try {
    const { slug } = req.params;

    const card = await Card.findOne({ slug }).select(
      'recipientName age message senderName theme createdAt'
    );

    if (!card) {
      return res.status(404).json({ error: 'Card not found.' });
    }

    return res.json({
      success: true,
      card: {
        recipientName: card.recipientName,
        age: card.age,
        message: card.message,
        senderName: card.senderName,
        theme: card.theme,
        createdAt: card.createdAt,
      },
    });
  } catch (err) {
    console.error('GET /api/cards/:slug error:', err);
    return res.status(500).json({ error: 'Internal server error.' });
  }
});

// ─── Start ────────────────────────────────────────────────────────────────────
app.listen(PORT, () => {
  console.log(`🚀 Server running on port ${PORT}`);
});
