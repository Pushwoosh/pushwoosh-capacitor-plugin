import { Pushwoosh } from 'pushwoosh-capacitor-plugin';
import { App } from '@capacitor/app';

const $ = (id) => document.getElementById(id);

function setStatus(success, text) {
  const dot = $('statusDot');
  const label = $('statusText');
  dot.classList.remove('status-dot--success', 'status-dot--error');
  dot.classList.add(success ? 'status-dot--success' : 'status-dot--error');
  label.textContent = text;
}

function showPushToken(token) {
  $('pushTokenDisplay').textContent = token;
  $('pushTokenContainer').classList.remove('hidden');
}

function showDeviceInfo(text) {
  const el = $('deviceInfoDisplay');
  el.textContent = text;
  el.classList.remove('hidden');
}

function openDeepLink(url) {
  if (!url) return;
  console.log('Deep link opened:', url);
  $('deepLinkUrl').textContent = url;
  $('deepLinkScreen').classList.remove('hidden');
}

async function initPushwoosh() {
  Pushwoosh.pushReceivedCallback((notification, err) => {
    if (err) {
      console.error('Failed to receive notification:', err);
    } else {
      console.log('Push Received:', JSON.stringify(notification));
    }
  });

  Pushwoosh.pushOpenedCallback((notification, err) => {
    if (err) {
      console.error('Failed to open notification:', err);
    } else {
      console.log('Push Opened:', JSON.stringify(notification));
    }
  });

  Pushwoosh.onDeviceReady({
    appid: 'XXXXX-XXXXX',
  });

  Pushwoosh.setMultiNotificationMode();

  try {
    await Pushwoosh.setApiToken('YOUR_API_TOKEN');
  } catch (e) {
    console.error('Failed to set API token:', e);
  }

  try {
    const result = await Pushwoosh.registerDevice();
    setStatus(true, 'Registered');
    showPushToken(result.pushToken);
    console.log('Device registered, token:', result.pushToken);
  } catch (error) {
    setStatus(false, 'Registration failed');
    console.error('Registration failed:', error);
  }
}

$('setUserIdBtn').addEventListener('click', async () => {
  const userId = $('userIdInput').value.trim();
  if (!userId) return;
  try {
    await Pushwoosh.setUserId(userId);
    console.log('User ID set to:', userId);
  } catch (error) {
    console.error('Failed to set User ID:', error);
  }
});

$('postEventBtn').addEventListener('click', async () => {
  const eventName = $('eventNameInput').value.trim();
  if (!eventName) return;
  try {
    await Pushwoosh.postEvent(eventName, { timestamp: new Date().toISOString() });
    console.log('Event posted:', eventName);
  } catch (error) {
    console.error('Failed to post event:', error);
  }
});

$('getPushTokenBtn').addEventListener('click', async () => {
  try {
    const result = await Pushwoosh.getPushToken();
    showDeviceInfo(result.value);
    console.log('Push Token:', result.value);
  } catch (error) {
    console.error('Failed to get push token:', error);
  }
});

$('getHwidBtn').addEventListener('click', async () => {
  try {
    const result = await Pushwoosh.getPushwooshHWID();
    showDeviceInfo(result.value);
    console.log('HWID:', result.value);
  } catch (error) {
    console.error('Failed to get HWID:', error);
  }
});

$('getStatusBtn').addEventListener('click', async () => {
  try {
    const result = await Pushwoosh.getRemoteNotificationStatus();
    showDeviceInfo(result.status);
    console.log('Notification status:', result.status);
  } catch (error) {
    console.error('Failed to get status:', error);
  }
});

$('deepLinkBackBtn').addEventListener('click', () => {
  $('deepLinkScreen').classList.add('hidden');
});

App.addListener('appUrlOpen', (event) => openDeepLink(event.url));

initPushwoosh();
