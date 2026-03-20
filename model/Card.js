const mongoose = require('mongoose');

const cardSchema = new mongoose.Schema({
  slug: {
    type: String,
    required: true,
    unique: true,
    index: true,
  },
  recipientName: {
    type: String,
    required: true,
    trim: true,
    maxlength: 50,
  },
  age: {
    type: Number,
    required: true,
    min: 1,
    max: 150,
  },
  message: {
    type: String,
    required: true,
    trim: true,
    maxlength: 500,
  },
  senderName: {
    type: String,
    trim: true,
    maxlength: 50,
    default: '',
  },
  theme: {
    type: String,
    enum: ['pink', 'blue', 'gold', 'green'],
    default: 'pink',
  },
  createdAt: {
    type: Date,
    default: Date.now,
    expires: 60 * 60 * 24 * 90, // auto-delete after 90 days
  },
});

module.exports = mongoose.model('Card', cardSchema);
