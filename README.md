# pushwoosh-capacitor-plugin

Pushwoosh plugin for Capacitor

## Install

### NPM Installation
```bash
npm install pushwoosh-capacitor-plugin@1.0.13
npx cap sync
```

### Git Installation
```bash
npm install git+https://github.com/Pushwoosh/pushwoosh-capacitor-plugin.git#1.0.13
npx cap sync
```

## Usage

### Initialization

```typescript
import { Pushwoosh } from 'pushwoosh-capacitor-plugin';

// 1. Register notification callbacks before initialization
Pushwoosh.pushReceivedCallback((notification, error) => {
  if (error) {
    console.error('Push receive error:', error);
    return;
  }
  console.log('Push received:', JSON.stringify(notification));
});

Pushwoosh.pushOpenedCallback((notification, error) => {
  if (error) {
    console.error('Push open error:', error);
    return;
  }
  console.log('Push opened:', JSON.stringify(notification));
});

// 2. Initialize Pushwoosh
Pushwoosh.onDeviceReady({
  appid: 'XXXXX-XXXXX',       // Pushwoosh Application ID
});

// 3. Register the device to receive push notifications
const { pushToken } = await Pushwoosh.registerDevice();
console.log('Registered with token:', pushToken);
```

### User ID and Events

```typescript
Pushwoosh.setUserId('user_123');

Pushwoosh.postEvent('purchase', {
  product: 'Premium Plan',
  price: '9.99',
});
```

### Tags

```typescript
// Set tags
await Pushwoosh.setTags({
  age: 25,
  name: 'John',
  favorite_categories: ['sports', 'news'],
});

// Get tags — resolves to a flat object: { age: 25, name: 'John', favorite_categories: ['sports', 'news'] }
const tags = await Pushwoosh.getTags();
console.log('Tags:', tags);
```

### Inbox

```typescript
// Open the native inbox screen, styled for your app
await Pushwoosh.presentInboxUI({
  barTitle: 'Notifications',
  dateFormat: 'dd.MM.yyyy',
  accentColor: '#ff6600',
  listEmptyMessage: 'Nothing here yet',
});

// Badge your own inbox button
const { value: unread } = await Pushwoosh.unreadMessagesCount();

// Build your own list on top of the inbox
const { messages } = await Pushwoosh.loadMessages();
for (const message of messages) {
  console.log(message.code, message.title, message.isRead);
}

// Act on a message from your own UI
await Pushwoosh.readMessage({ code: messages[0].code });
await Pushwoosh.performAction({ code: messages[0].code });
await Pushwoosh.deleteMessages({ codes: messages.map((m) => m.code) });
```

Style images are addressed by a path relative to your app's `webDir`, so they can sit next to the
rest of your web assets: `{ defaultImageIcon: 'assets/inbox-icon.png' }`. Colors accept `#RGB`,
`#RRGGBB`, `#AARRGGBB`, `0xRRGGBB` and `0xAARRGGBB`. A value a platform cannot parse is ignored
with a console warning and the rest of the style still applies.

On the web, every inbox method resolves with an empty value instead of throwing, so one codebase
runs on all three targets.

## API

<docgen-index>

* [`onDeviceReady(...)`](#ondeviceready)
* [`registerDevice()`](#registerdevice)
* [`unregisterDevice()`](#unregisterdevice)
* [`additionalAuthorizationOptions(...)`](#additionalauthorizationoptions)
* [`setTags(...)`](#settags)
* [`getTags()`](#gettags)
* [`getPushToken()`](#getpushtoken)
* [`getPushwooshHWID()`](#getpushwooshhwid)
* [`getRemoteNotificationStatus()`](#getremotenotificationstatus)
* [`setApplicationIconBadgeNumber(...)`](#setapplicationiconbadgenumber)
* [`getApplicationIconBadgeNumber()`](#getapplicationiconbadgenumber)
* [`addToApplicationIconBadgeNumber(...)`](#addtoapplicationiconbadgenumber)
* [`getLaunchNotification()`](#getlaunchnotification)
* [`clearLaunchNotification()`](#clearlaunchnotification)
* [`setUserId(...)`](#setuserid)
* [`setLanguage(...)`](#setlanguage)
* [`setApiToken(...)`](#setapitoken)
* [`postEvent(...)`](#postevent)
* [`setMultiNotificationMode()`](#setmultinotificationmode)
* [`setSingleNotificationMode()`](#setsinglenotificationmode)
* [`pushReceivedCallback(...)`](#pushreceivedcallback)
* [`pushOpenedCallback(...)`](#pushopenedcallback)
* [`presentInboxUI(...)`](#presentinboxui)
* [`loadMessages()`](#loadmessages)
* [`messagesCount()`](#messagescount)
* [`unreadMessagesCount()`](#unreadmessagescount)
* [`messagesWithNoActionPerformedCount()`](#messageswithnoactionperformedcount)
* [`readMessage(...)`](#readmessage)
* [`readMessages(...)`](#readmessages)
* [`deleteMessage(...)`](#deletemessage)
* [`deleteMessages(...)`](#deletemessages)
* [`performAction(...)`](#performaction)
* [`setCommunicationEnabled(...)`](#setcommunicationenabled)
* [`isCommunicationEnabled(...)`](#iscommunicationenabled)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### onDeviceReady(...)

```typescript
onDeviceReady(config: PushwooshConfig) => void
```

| Param        | Type                                                        |
| ------------ | ----------------------------------------------------------- |
| **`config`** | <code><a href="#pushwooshconfig">PushwooshConfig</a></code> |

--------------------


### registerDevice()

```typescript
registerDevice() => Promise<{ pushToken: string; }>
```

**Returns:** <code>Promise&lt;{ pushToken: string; }&gt;</code>

--------------------


### unregisterDevice()

```typescript
unregisterDevice() => Promise<void>
```

--------------------


### additionalAuthorizationOptions(...)

```typescript
additionalAuthorizationOptions(options: AuthOptions) => void
```

| Param         | Type                                                |
| ------------- | --------------------------------------------------- |
| **`options`** | <code><a href="#authoptions">AuthOptions</a></code> |

--------------------


### setTags(...)

```typescript
setTags(config: PushwooshTags) => Promise<void>
```

| Param        | Type                                                    |
| ------------ | ------------------------------------------------------- |
| **`config`** | <code><a href="#pushwooshtags">PushwooshTags</a></code> |

--------------------


### getTags()

```typescript
getTags() => Promise<PushwooshTags>
```

Reads the device tags from Pushwoosh.

Resolves to the tags as a flat object (tag name → value), with no `tags` envelope —
the same shape the native SDKs and the other Pushwoosh plugins return.
Rejects when the native read fails.

**Returns:** <code>Promise&lt;<a href="#pushwooshtags">PushwooshTags</a>&gt;</code>

--------------------


### getPushToken()

```typescript
getPushToken() => Promise<{ value: string | null; }>
```

**Returns:** <code>Promise&lt;{ value: string | null; }&gt;</code>

--------------------


### getPushwooshHWID()

```typescript
getPushwooshHWID() => Promise<{ value: string | null; }>
```

**Returns:** <code>Promise&lt;{ value: string | null; }&gt;</code>

--------------------


### getRemoteNotificationStatus()

```typescript
getRemoteNotificationStatus() => Promise<{ status: RemoteNotificationStatus; }>
```

**Returns:** <code>Promise&lt;{ status: <a href="#remotenotificationstatus">RemoteNotificationStatus</a>; }&gt;</code>

--------------------


### setApplicationIconBadgeNumber(...)

```typescript
setApplicationIconBadgeNumber(badge: number) => void
```

| Param       | Type                |
| ----------- | ------------------- |
| **`badge`** | <code>number</code> |

--------------------


### getApplicationIconBadgeNumber()

```typescript
getApplicationIconBadgeNumber() => Promise<{ badge: number; }>
```

**Returns:** <code>Promise&lt;{ badge: number; }&gt;</code>

--------------------


### addToApplicationIconBadgeNumber(...)

```typescript
addToApplicationIconBadgeNumber(badge: number) => void
```

| Param       | Type                |
| ----------- | ------------------- |
| **`badge`** | <code>number</code> |

--------------------


### getLaunchNotification()

```typescript
getLaunchNotification() => Promise<{ notification: string; }>
```

Android resolves the launch notification as a JSON string under `value`, and resolves nothing
when the app was not opened from a push, so the promise settles as `undefined`. iOS resolves
the raw notification dictionary under `notification`, or `null` in that same case. The declared
shape below matches neither platform; the payload mismatch is tracked separately.

**Returns:** <code>Promise&lt;{ notification: string; }&gt;</code>

--------------------


### clearLaunchNotification()

```typescript
clearLaunchNotification() => void
```

--------------------


### setUserId(...)

```typescript
setUserId(userId: string) => void
```

| Param        | Type                |
| ------------ | ------------------- |
| **`userId`** | <code>string</code> |

--------------------


### setLanguage(...)

```typescript
setLanguage(language: string) => void
```

| Param          | Type                |
| -------------- | ------------------- |
| **`language`** | <code>string</code> |

--------------------


### setApiToken(...)

```typescript
setApiToken(token: string) => void
```

| Param       | Type                |
| ----------- | ------------------- |
| **`token`** | <code>string</code> |

--------------------


### postEvent(...)

```typescript
postEvent(event: string, attributes?: Record<string, string> | undefined) => void
```

| Param            | Type                                                            |
| ---------------- | --------------------------------------------------------------- |
| **`event`**      | <code>string</code>                                             |
| **`attributes`** | <code><a href="#record">Record</a>&lt;string, string&gt;</code> |

--------------------


### setMultiNotificationMode()

```typescript
setMultiNotificationMode() => Promise<void>
```

--------------------


### setSingleNotificationMode()

```typescript
setSingleNotificationMode() => Promise<void>
```

--------------------


### pushReceivedCallback(...)

```typescript
pushReceivedCallback(callback: PushwooshNotificationCallback) => Promise<CallbackID>
```

| Param          | Type                                                                                    |
| -------------- | --------------------------------------------------------------------------------------- |
| **`callback`** | <code><a href="#pushwooshnotificationcallback">PushwooshNotificationCallback</a></code> |

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### pushOpenedCallback(...)

```typescript
pushOpenedCallback(callback: PushwooshNotificationCallback) => Promise<CallbackID>
```

| Param          | Type                                                                                    |
| -------------- | --------------------------------------------------------------------------------------- |
| **`callback`** | <code><a href="#pushwooshnotificationcallback">PushwooshNotificationCallback</a></code> |

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### presentInboxUI(...)

```typescript
presentInboxUI(style?: InboxStyle | undefined) => Promise<void>
```

Opens the native inbox screen of the Pushwoosh SDK.

The style of each call is the SDK defaults plus the keys passed here — keys of a previous
call do not carry over. Resolves once the screen is on display, and rejects with the reason
in the message when it cannot be: no view controller or activity to present it from, or a
presentation of the inbox already under way.

| Param       | Type                                              |
| ----------- | ------------------------------------------------- |
| **`style`** | <code><a href="#inboxstyle">InboxStyle</a></code> |

--------------------


### loadMessages()

```typescript
loadMessages() => Promise<{ messages: InboxNotification[]; }>
```

Reads the inbox from the server. An empty inbox resolves an empty array, not an error.

**Returns:** <code>Promise&lt;{ messages: InboxNotification[]; }&gt;</code>

--------------------


### messagesCount()

```typescript
messagesCount() => Promise<{ value: number; }>
```

Total number of inbox messages.

**Returns:** <code>Promise&lt;{ value: number; }&gt;</code>

--------------------


### unreadMessagesCount()

```typescript
unreadMessagesCount() => Promise<{ value: number; }>
```

Number of inbox messages the user has not read.

**Returns:** <code>Promise&lt;{ value: number; }&gt;</code>

--------------------


### messagesWithNoActionPerformedCount()

```typescript
messagesWithNoActionPerformedCount() => Promise<{ value: number; }>
```

Number of inbox messages whose action has not been performed.

**Returns:** <code>Promise&lt;{ value: number; }&gt;</code>

--------------------


### readMessage(...)

```typescript
readMessage(options: { code: string; }) => Promise<void>
```

Marks one message as read.

| Param         | Type                           |
| ------------- | ------------------------------ |
| **`options`** | <code>{ code: string; }</code> |

--------------------


### readMessages(...)

```typescript
readMessages(options: { codes: string[]; }) => Promise<void>
```

Marks a batch of messages as read.

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ codes: string[]; }</code> |

--------------------


### deleteMessage(...)

```typescript
deleteMessage(options: { code: string; }) => Promise<void>
```

Deletes one message from the inbox.

| Param         | Type                           |
| ------------- | ------------------------------ |
| **`options`** | <code>{ code: string; }</code> |

--------------------


### deleteMessages(...)

```typescript
deleteMessages(options: { codes: string[]; }) => Promise<void>
```

Deletes a batch of messages from the inbox.

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ codes: string[]; }</code> |

--------------------


### performAction(...)

```typescript
performAction(options: { code: string; }) => Promise<void>
```

Performs the message's own action — opens its rich media, URL or deep link.

| Param         | Type                           |
| ------------- | ------------------------------ |
| **`options`** | <code>{ code: string; }</code> |

--------------------


### setCommunicationEnabled(...)

```typescript
setCommunicationEnabled(enabled: boolean) => Promise<{ result: void | string; }>
```

| Param         | Type                 |
| ------------- | -------------------- |
| **`enabled`** | <code>boolean</code> |

**Returns:** <code>Promise&lt;{ result: string | void; }&gt;</code>

--------------------


### isCommunicationEnabled(...)

```typescript
isCommunicationEnabled(success: (enabled: boolean) => void) => void
```

| Param         | Type                                       |
| ------------- | ------------------------------------------ |
| **`success`** | <code>(enabled: boolean) =&gt; void</code> |

--------------------


### Interfaces


#### PushwooshConfig

| Prop              | Type                |
| ----------------- | ------------------- |
| **`appid`**       | <code>string</code> |
| **`serviceName`** | <code>string</code> |


#### PushwooshTags


#### InboxStyle

Appearance of the native inbox screen, applied per `presentInboxUI` call.

Colors accept `#RGB`, `#RRGGBB`, `#AARRGGBB`, `0xRRGGBB` and `0xAARRGGBB`. The hex digits may be
upper or lower case; the `0x` prefix must be lowercase.
Images are addressed by a path relative to your app's `webDir`, so they can live next to the
rest of your web assets. A value the platform cannot parse is ignored with a console warning
and the remaining keys still apply.

| Prop                       | Type                | Description                                                                          |
| -------------------------- | ------------------- | ------------------------------------------------------------------------------------ |
| **`dateFormat`**           | <code>string</code> | Date pattern of the message date. Android `SimpleDateFormat`, iOS `NSDateFormatter`. |
| **`barTitle`**             | <code>string</code> | Title of the navigation bar.                                                         |
| **`listEmptyMessage`**     | <code>string</code> | Text shown when the inbox is empty.                                                  |
| **`listErrorMessage`**     | <code>string</code> | Text shown when the inbox fails to load.                                             |
| **`defaultImageIcon`**     | <code>string</code> | Icon shown next to a message that carries no image of its own.                       |
| **`listEmptyImage`**       | <code>string</code> | Image shown when the inbox is empty.                                                 |
| **`listErrorImage`**       | <code>string</code> | Image shown when the inbox fails to load.                                            |
| **`accentColor`**          | <code>string</code> |                                                                                      |
| **`backgroundColor`**      | <code>string</code> |                                                                                      |
| **`highlightColor`**       | <code>string</code> | Color of the pressed state of a row.                                                 |
| **`titleColor`**           | <code>string</code> |                                                                                      |
| **`descriptionColor`**     | <code>string</code> |                                                                                      |
| **`dateColor`**            | <code>string</code> |                                                                                      |
| **`dividerColor`**         | <code>string</code> | Color of the separator between rows.                                                 |
| **`barBackgroundColor`**   | <code>string</code> |                                                                                      |
| **`barAccentColor`**       | <code>string</code> |                                                                                      |
| **`barTextColor`**         | <code>string</code> |                                                                                      |
| **`readTitleColor`**       | <code>string</code> | Android only: title color of a read message.                                         |
| **`readDescriptionColor`** | <code>string</code> | Android only: text color of a read message.                                          |
| **`readDateColor`**        | <code>string</code> | Android only: date color of a read message.                                          |
| **`unreadImage`**          | <code>string</code> | iOS only: the unread marker image.                                                   |
| **`defaultTextColor`**     | <code>string</code> | iOS only: the default text color.                                                    |


### Type Aliases


#### AuthOptions

<code><a href="#record">Record</a>&lt;string, number|string&gt;</code>


#### Record

Construct a type with a set of properties K of type T

<code>{ [P in K]: T; }</code>


#### RemoteNotificationStatus

<code><a href="#record">Record</a>&lt;string,string|number|boolean&gt;</code>


#### PushwooshNotificationCallback

<code>(message: string | null, err?: any): void</code>


#### CallbackID

<code>string</code>


#### InboxNotification

One message of the Message Inbox, as both native platforms report it.

A field the native message does not carry is omitted, not sent as an empty string or null —
read it with `?.` or `??`, not with a truthiness check on a guaranteed key.

<code>{ /** Inbox message identifier. The only guaranteed field; every mutator takes it. */ code: string; title?: string; message?: string; /** Icon shown next to the message in the inbox list. */ imageUrl?: string; /** Banner image of the message, taken from the `b` action parameter on both platforms. */ bannerUrl?: string; /** ISO 8601 with a timezone offset, e.g. `2026-10-07T14:32:05+0300`. */ sendDate?: string; /** * Message type, with the same codes on Android and iOS: * 0 plain, 1 rich media, 2 URL, 3 deep link. */ type?: number; isRead?: boolean; isActionPerformed?: boolean; /** Raw action parameters of the message as sent by Pushwoosh. */ actionParams?: <a href="#record">Record</a>&lt;string, unknown&gt;; /** Custom data attached to the push, taken from the `u` action parameter. */ customData?: <a href="#record">Record</a>&lt;string, unknown&gt;; }</code>

</docgen-api>
