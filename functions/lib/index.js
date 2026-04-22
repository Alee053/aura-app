"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.sendTestNotification = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
exports.sendTestNotification = functions.https.onRequest(async (req, res) => {
    // CORS headers for local testing
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type');
    if (req.method === 'OPTIONS') {
        res.status(204).send('');
        return;
    }
    if (req.method !== 'POST') {
        res.status(405).send('Method not allowed');
        return;
    }
    const { title = 'Test Notification', body = 'This is a test push notification' } = req.body;
    try {
        await admin.messaging().send({
            notification: { title, body },
            topic: 'test-notifications'
        });
        res.status(200).json({ success: true, message: 'Notification sent' });
    }
    catch (error) {
        console.error('Error sending notification:', error);
        res.status(500).json({ success: false, error: 'Failed to send notification' });
    }
});
//# sourceMappingURL=index.js.map