require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

const PORT = process.env.PORT || 3000;
const STUN_SERVERS = (process.env.STUN_SERVERS || 'stun:stun.l.google.com:19302').split(',');

// In-memory registry (No database, strict privacy)
// phoneHash -> { deviceId, socketId, publicKey, isOnline }
const usersByPhone = new Map();
// deviceId -> { phoneHash, socketId, publicKey, isOnline }
const devices = new Map();
// socketId -> deviceId
const socketToDevice = new Map();
// deviceId -> Set of socketIds (who is watching this device's presence)
const presenceSubscriptions = new Map();

// Helper to notify subscribers when a device's presence changes
function notifyPresenceChange(deviceId, isOnline) {
  const subscribers = presenceSubscriptions.get(deviceId);
  if (subscribers && subscribers.size > 0) {
    const eventName = isOnline ? 'peer-online' : 'peer-offline';
    const deviceData = devices.get(deviceId);
    const phoneHash = deviceData ? deviceData.phoneHash : null;
    
    console.log(`[Presence] Notifying subscribers: ${deviceId} is now ${isOnline ? 'ONLINE' : 'OFFLINE'}`);
    
    for (const subSocketId of subscribers) {
      io.to(subSocketId).emit(eventName, {
        deviceId: deviceId,
        phoneHash: phoneHash
      });
    }
  }
}

io.on('connection', (socket) => {
  console.log(`[Socket] Client connected: ${socket.id}`);

  // 1. Device registration
  socket.on('register-device', (data) => {
    const { phoneHash, deviceId, publicKey } = data;
    if (!phoneHash || !deviceId || !publicKey) {
      console.warn(`[Registry] Invalid registration attempt from ${socket.id}`);
      return;
    }

    console.log(`[Registry] Registering device: ${deviceId} (Hashed phone prefix: ${phoneHash.substring(0, 8)}...)`);

    // Clean up any old registration for this device
    const existing = devices.get(deviceId);
    if (existing && existing.socketId !== socket.id) {
      // Disconnect or override old socket mapping
      socketToDevice.delete(existing.socketId);
    }

    const deviceRecord = {
      phoneHash,
      socketId: socket.id,
      publicKey,
      isOnline: true
    };

    devices.set(deviceId, deviceRecord);
    usersByPhone.set(phoneHash, deviceRecord);
    socketToDevice.set(socket.id, deviceId);

    // Respond with success and STUN server config
    socket.emit('register-response', {
      success: true,
      stunServers: STUN_SERVERS
    });

    // Notify peers watching this device
    notifyPresenceChange(deviceId, true);
  });

  // 2. Query contact presence & register subscription
  socket.on('check-contact', (data, callback) => {
    const { phoneHash } = data;
    const requesterDeviceId = socketToDevice.get(socket.id);
    
    if (!phoneHash) {
      if (typeof callback === 'function') callback({ error: 'Missing phoneHash' });
      return;
    }

    console.log(`[Contact] Lookup requested for phone hash starting with: ${phoneHash.substring(0, 8)}...`);
    const registeredUser = usersByPhone.get(phoneHash);

    if (registeredUser) {
      // Subscribe this requester to target's presence updates
      const targetDeviceId = registeredUser.deviceId;
      if (!presenceSubscriptions.has(targetDeviceId)) {
        presenceSubscriptions.set(targetDeviceId, new Set());
      }
      presenceSubscriptions.get(targetDeviceId).add(socket.id);

      console.log(`[Presence] Socket ${socket.id} (device ${requesterDeviceId}) subscribed to presence of ${targetDeviceId}`);

      if (typeof callback === 'function') {
        callback({
          registered: true,
          deviceId: targetDeviceId,
          publicKey: registeredUser.publicKey,
          isOnline: registeredUser.isOnline
        });
      }
    } else {
      if (typeof callback === 'function') {
        callback({ registered: false });
      }
    }
  });

  // 3. Connection Request (Initiate signalling negotiation)
  socket.on('connection-request', (data) => {
    const senderDeviceId = socketToDevice.get(socket.id);
    const { targetDeviceId } = data;
    if (!senderDeviceId || !targetDeviceId) return;

    console.log(`[Signaling] Connection request from ${senderDeviceId} to ${targetDeviceId}`);
    const target = devices.get(targetDeviceId);
    if (target && target.isOnline) {
      io.to(target.socketId).emit('connection-request', { senderDeviceId });
    } else {
      socket.emit('connection-error', { targetDeviceId, reason: 'Peer offline' });
    }
  });

  // 4. WebRTC Offer relay
  socket.on('webrtc-offer', (data) => {
    const senderDeviceId = socketToDevice.get(socket.id);
    const { targetDeviceId, sdp } = data;
    if (!senderDeviceId || !targetDeviceId || !sdp) return;

    // Secure practice: Never log the actual SDP content to logs
    console.log(`[Signaling] Offer relayed from ${senderDeviceId} to ${targetDeviceId}`);
    const target = devices.get(targetDeviceId);
    if (target && target.isOnline) {
      io.to(target.socketId).emit('webrtc-offer', {
        senderDeviceId,
        sdp // Relayed to destination client only
      });
    }
  });

  // 5. WebRTC Answer relay
  socket.on('webrtc-answer', (data) => {
    const senderDeviceId = socketToDevice.get(socket.id);
    const { targetDeviceId, sdp } = data;
    if (!senderDeviceId || !targetDeviceId || !sdp) return;

    console.log(`[Signaling] Answer relayed from ${senderDeviceId} to ${targetDeviceId}`);
    const target = devices.get(targetDeviceId);
    if (target && target.isOnline) {
      io.to(target.socketId).emit('webrtc-answer', {
        senderDeviceId,
        sdp
      });
    }
  });

  // 6. ICE Candidate relay
  socket.on('ice-candidate', (data) => {
    const senderDeviceId = socketToDevice.get(socket.id);
    const { targetDeviceId, candidate } = data;
    if (!senderDeviceId || !targetDeviceId || !candidate) return;

    // Secure practice: Never log the actual ICE candidate details to logs
    console.log(`[Signaling] ICE Candidate relayed from ${senderDeviceId} to ${targetDeviceId}`);
    const target = devices.get(targetDeviceId);
    if (target && target.isOnline) {
      io.to(target.socketId).emit('ice-candidate', {
        senderDeviceId,
        candidate
      });
    }
  });

  // 7. Connection Error signaling
  socket.on('connection-error', (data) => {
    const senderDeviceId = socketToDevice.get(socket.id);
    const { targetDeviceId, reason } = data;
    if (!senderDeviceId || !targetDeviceId) return;

    console.log(`[Signaling] Connection error reported by ${senderDeviceId} to ${targetDeviceId}: ${reason}`);
    const target = devices.get(targetDeviceId);
    if (target && target.isOnline) {
      io.to(target.socketId).emit('connection-error', {
        senderDeviceId,
        reason
      });
    }
  });

  // 8. Disconnect handling
  socket.on('disconnect', () => {
    console.log(`[Socket] Client disconnected: ${socket.id}`);
    const deviceId = socketToDevice.get(socket.id);
    if (deviceId) {
      const deviceRecord = devices.get(deviceId);
      if (deviceRecord) {
        deviceRecord.isOnline = false;
        deviceRecord.socketId = null;
        
        // Notify subscribers
        notifyPresenceChange(deviceId, false);
      }
      socketToDevice.delete(socket.id);
    }

    // Clean up subscriptions where this socket was the observer
    for (const [targetDeviceId, subscribers] of presenceSubscriptions.entries()) {
      if (subscribers.has(socket.id)) {
        subscribers.delete(socket.id);
        console.log(`[Presence] Removed ${socket.id} subscription to ${targetDeviceId}`);
      }
    }
  });
});

// Basic HTTP status endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'OK', activeDevices: devices.size });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[Server] Signaling and presence server running on port ${PORT}`);
});
