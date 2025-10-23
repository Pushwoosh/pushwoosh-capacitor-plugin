# Pushwoosh Capacitor Plugin Example App

This example app demonstrates how to use the Pushwoosh Capacitor plugin in your Capacitor application.

## Quick Start

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Install the Pushwoosh plugin:**
   ```bash
   npm install pushwoosh-capacitor-plugin
   ```

3. **Sync with native platforms:**
   ```bash
   npx cap sync
   ```

4. **Run the app:**
   ```bash
   # For web development
   npm start
   
   # For iOS
   npx cap run ios
   
   # For Android  
   npx cap run android
   ```

## Configuration

Before running the app, you need to:

1. Replace the `projectid` and `appid` in `src/js/example.js` with your Pushwoosh credentials
2. Add your `google-services.json` (Android) and `GoogleService-Info.plist` (iOS) files
3. Configure push notification capabilities in your native projects

## Features Demonstrated

The example app shows how to:

- ✅ Initialize Pushwoosh SDK
- ✅ Register device for push notifications
- ✅ Handle push notification callbacks
- ✅ Set user ID, language, and API tokens
- ✅ Post custom events
- ✅ Manage application badge numbers
- ✅ Control communication settings

## File Structure

- `src/js/example.js` - Main plugin demonstration code
- `src/index.html` - Simple HTML interface
- `android/` - Android native project
- `ios/` - iOS native project
- `capacitor.config.json` - Capacitor configuration

## Learn More

For complete documentation, visit: https://github.com/pushwoosh/pushwoosh-capacitor-plugin
