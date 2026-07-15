# PersonalChat Android Client

This is the Android-first client for **PersonalChat**, a private 1-to-1 messaging proof-of-concept. It utilizes MVVM architecture, SQLCipher-encrypted Room database, and a direct WebRTC DataChannel connection for zero-server messaging.

## Tech Stack & Architecture

- **Language**: Java
- **Target OS**: Android 6.0+ (API 23+)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository pattern
- **Local Encryption**: SQLite database encrypted with **SQLCipher** (via Room integration)
- **Key Protection**: **Android Keystore System** used to protect the SQLCipher passphrase and device identity RSA keypair
- **P2P Transport**: **Google WebRTC** DataChannel for messaging and delivery status updates
- **Signaling Relays**: **Socket.IO Client** for presence and WebRTC SDP/ICE exchange

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (or newer)
- Android SDK 33
- A physical Android device or two emulator instances running on the same local network / host

### Android Studio Setup
1. Open Android Studio.
2. Select **File > Open** and choose the `android-client` directory.
3. Allow Gradle to sync and download dependencies.
4. Verify the following dependencies are successfully retrieved:
   - `androidx.room:room-runtime:2.5.2`
   - `net.zetetic:android-database-sqlcipher:4.5.4@aar`
   - `io.socket:socket.io-client:2.1.0`
   - `org.webrtc:google-webrtc:1.0.32006`

---

## Configuration & Run

### 1. Signaling URL
By default, the client points to the Android Emulator loopback alias:
`http://10.0.2.2:3000`

If you are running the signaling server on a custom server or LAN, you can edit the **Signaling Server URL** dynamically under the **Settings/Profile** screen in the application.

### 2. Multi-Device Emulator Configuration for Testing
To test peer-to-peer data channels between two emulators on the same machine:
1. Start two Android emulators (e.g. Emulator 5554 and Emulator 5556).
2. Start the signaling server locally (`npm start` on port 3000).
3. Install and run PersonalChat on both emulators.
4. On **Emulator 5554**, complete onboarding (e.g., Name: `Alice`, Phone: `+15550000001`).
5. On **Emulator 5556**, complete onboarding (e.g., Name: `Bob`, Phone: `+15550000002`).
6. On **Emulator 5554**, tap the "+" button, type `+15550000002`, and tap **Start Conversation**.
7. The app queries the signaling registry, performs a hashed match, exchanges SDP over Socket.IO, establishing a **Direct WebRTC connection** (displays "Direct Connection established").
8. Send messages back and forth!

---

## Core Security Features

1. **Keystore Passphrase Encryption**: 
   A random 256-bit passphrase is created on first launch. It is encrypted via `AES/GCM/NoPadding` using a master key stored in the **Android Keystore**, and the ciphertext is stored in SharedPreferences. Upon launch, the Keystore decrypts the key to feed SQLCipher.
2. **P2P Identity Exchange**: 
   The user's display name and phone number are exchanged directly peer-to-peer over the secure WebRTC DataChannel once the connection is established. The signaling server is only aware of the connection target device IDs.
3. **No Raw Phone Numbers on Server**: 
   Phone numbers are normalized and converted to SHA-256 hashes before being registered or queried.
4. **No Console Logs**: 
   Production logs must never output raw phone numbers, SDP values, or text message bodies.

---

## Unit Testing
Run the local unit tests in Android Studio by right-clicking on `app/src/test` and selecting **Run 'All Tests'** or run via command line:
```bash
./gradlew test
```

---

## Future Roadmap & TODOs
- [ ] **TODO: Phone OTP Login**: Integrate a SMS OTP provider (e.g., Firebase Auth or custom gateway) for robust phone number verification, keeping authentication modular.
- [ ] **TODO: TURN Relay Support**: Add a TURN server configuration to the WebRTC connection config to support networks behind symmetric NATs.
- [ ] **TODO: End-to-End Encryption**: Add application-layer E2EE (e.g., Signal Protocol / Double Ratchet) so that even if WebRTC transport is intercepted, message content remains fully encrypted.
- [ ] **TODO: Push Notifications**: Add FCM (Firebase Cloud Messaging) integration to wake up the client when a peer wants to initiate a signaling handshake.
- [ ] **TODO: P2P Audio/Video Calling**: Extend WebRTC peer connection configuration to capture audio/video tracks.
