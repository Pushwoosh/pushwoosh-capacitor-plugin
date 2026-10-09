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

function showTags(text) {
  const el = $('tagsDisplay');
  el.textContent = text;
  el.classList.remove('hidden');
}

$('setTagBtn').addEventListener('click', async () => {
  const key = $('tagKeyInput').value.trim();
  const value = $('tagValueInput').value.trim();
  if (!key) return;
  try {
    await Pushwoosh.setTags({ [key]: value });
    showTags(`Set ${key} = ${value}`);
    console.log('Tag set:', key, value);
  } catch (error) {
    showTags(`Failed to set tag: ${error}`);
    console.error('Failed to set tag:', error);
  }
});

$('getTagsBtn').addEventListener('click', async () => {
  try {
    const tags = await Pushwoosh.getTags();
    showTags(JSON.stringify(tags));
    console.log('Tags:', JSON.stringify(tags));
  } catch (error) {
    showTags(`Failed to get tags: ${error}`);
    console.error('Failed to get tags:', error);
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

// iOS resolves an object of "0"/"1" strings, Android a bare "0"/"1" string.
function describeNotificationStatus(status) {
  if (status === null || status === undefined) return 'Notifications enabled: unknown';
  const enabled = typeof status === 'object' ? status.enabled : status;
  const yesNo = enabled === undefined ? 'unknown' : (String(enabled) === '1' || enabled === true ? 'yes' : 'no');
  const headline = `Notifications enabled: ${yesNo}`;
  if (typeof status !== 'object') return headline;
  const details = Object.entries(status).filter(([key]) => key !== 'enabled').map(([key, value]) => `${key}: ${value}`).join(', ');
  return details ? `${headline} (${details})` : headline;
}

$('getStatusBtn').addEventListener('click', async () => {
  try {
    const result = await Pushwoosh.getRemoteNotificationStatus();
    showDeviceInfo(describeNotificationStatus(result.status));
    console.log('Notification status:', JSON.stringify(result.status));
  } catch (error) {
    console.error('Failed to get status:', error);
  }
});

function showInbox(text) {
  const el = $('inboxDisplay');
  el.textContent = text;
  el.classList.remove('hidden');
}

$('showInboxBtn').addEventListener('click', async () => {
  try {
    await Pushwoosh.presentInboxUI({ barTitle: 'Demo Inbox', dateFormat: 'dd.MM.yyyy' });
    console.log('Inbox presented');
  } catch (error) {
    showInbox(`Error: ${error.message ?? error}`);
    console.error('Failed to present inbox:', error);
  }
});

$('inboxCountsBtn').addEventListener('click', async () => {
  try {
    const total = await Pushwoosh.messagesCount();
    const unread = await Pushwoosh.unreadMessagesCount();
    showInbox(`Total: ${total.value}, Unread: ${unread.value}`);
    console.log('Inbox counts:', total.value, unread.value);
  } catch (error) {
    showInbox(`Error: ${error.message ?? error}`);
    console.error('Failed to get inbox counts:', error);
  }
});

$('loadMessagesBtn').addEventListener('click', async () => {
  try {
    const { messages } = await Pushwoosh.loadMessages();
    if (messages.length === 0) {
      showInbox('No inbox messages');
    } else {
      showInbox(messages.map((m) => `[${m.isRead ? 'read' : 'unread'}] ${m.title ?? ''}`).join('\n'));
    }
    console.log('Inbox messages:', JSON.stringify(messages));
  } catch (error) {
    showInbox(`Error: ${error.message ?? error}`);
    console.error('Failed to load inbox messages:', error);
  }
});

$('deepLinkBackBtn').addEventListener('click', () => {
  $('deepLinkScreen').classList.add('hidden');
});

App.addListener('appUrlOpen', (event) => openDeepLink(event.url));

initPushwoosh();
