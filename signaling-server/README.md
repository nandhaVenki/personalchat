# PersonalChat Signaling & Presence Server

This is the signaling and presence server for the **PersonalChat** direct 1-to-1 messaging system. It acts as an in-memory directory and relay for WebRTC signaling (SDP offer/answer and ICE candidates).

## Privacy Features
1. **Zero Message Logging**: This server does not receive, log, or persist text messages, attachments, or conversation histories. Communication is strictly peer-to-peer over WebRTC.
2. **In-Memory Registry**: No database is used. All active user registrations, socket links, and online states are stored in-memory and lost upon server restart.
3. **No Raw Phone Numbers**: The server only receives cryptographic hashes (SHA-256) of normalized phone numbers.
4. **No Contact List Uploads**: The client query mechanism performs a single hashed lookup; it never uploads or stores complete contact books.

## Setup & Running

### Prerequisites
- Node.js (v16+)
- npm (v7+)

### Installation
1. Navigate to the `signaling-server` directory:
   ```bash
   cd signaling-server
   ```
2. Install dependencies:
   ```bash
   npm install
   ```

### Configuration
Create a `.env` file from the template:
```bash
cp .env.example .env
```

Parameters:
- `PORT`: The port the server runs on (default: `3000`).
- `STUN_SERVERS`: Comma-separated list of STUN servers to serve to clients (default: `stun:stun.l.google.com:19302`).

### Running the Server
Start the server:
```bash
npm start
```
By default, the server runs on `http://localhost:3000`.

## Socket.IO Interface Events

### Client -> Server
- `register-device`: Register client presence and encryption identity.
  - Payload: `{ phoneHash: String, deviceId: String, publicKey: String }`
- `check-contact`: Check if a hashed contact is registered and get online details.
  - Payload: `{ phoneHash: String }`
  - Callback: Returns `{ registered: Boolean, deviceId: String, publicKey: String, isOnline: Boolean }`
- `connection-request`: Ask the peer to initialize WebRTC negotiation.
  - Payload: `{ targetDeviceId: String }`
- `webrtc-offer`: Relay an SDP offer to target.
  - Payload: `{ targetDeviceId: String, sdp: String }`
- `webrtc-answer`: Relay an SDP answer to target.
  - Payload: `{ targetDeviceId: String, sdp: String }`
- `ice-candidate`: Relay an ICE candidate to target.
  - Payload: `{ targetDeviceId: String, candidate: Object }`
- `connection-error`: Notify a peer of negotiation failures.
  - Payload: `{ targetDeviceId: String, reason: String }`

### Server -> Client
- `register-response`: Confirmation of registration.
  - Payload: `{ success: true, stunServers: Array }`
- `peer-online`: Fired when a contact device comes online.
  - Payload: `{ deviceId: String, phoneHash: String }`
- `peer-offline`: Fired when a contact device goes offline.
  - Payload: `{ deviceId: String, phoneHash: String }`
- `connection-request`: Forwarded request to start WebRTC session.
  - Payload: `{ senderDeviceId: String }`
- `webrtc-offer`: Forwarded WebRTC SDP offer.
  - Payload: `{ senderDeviceId: String, sdp: String }`
- `webrtc-answer`: Forwarded WebRTC SDP answer.
  - Payload: `{ senderDeviceId: String, sdp: String }`
- `ice-candidate`: Forwarded WebRTC ICE candidate.
  - Payload: `{ senderDeviceId: String, candidate: Object }`
- `connection-error`: Forwarded connection failure report.
  - Payload: `{ senderDeviceId: String, reason: String }`
