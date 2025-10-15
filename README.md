# pushwoosh-capacitor-plugin

Pushwoosh plugin for Capacitor

## Install

```bash
npm install pushwoosh-capacitor-plugin
npx cap sync
```

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
getTags() => Promise<{ tags: PushwooshTags; }>
```

**Returns:** <code>Promise&lt;{ tags: <a href="#pushwooshtags">PushwooshTags</a>; }&gt;</code>

--------------------


### getPushToken()

```typescript
getPushToken() => { value: string | null; }
```

**Returns:** <code>{ value: string | null; }</code>

--------------------


### getPushwooshHWID()

```typescript
getPushwooshHWID() => { value: string | null; }
```

**Returns:** <code>{ value: string | null; }</code>

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
getLaunchNotification() => { notification: string; }
```

**Returns:** <code>{ notification: string; }</code>

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
presentInboxUI(params?: Record<string, any> | undefined) => void
```

| Param        | Type                                                         |
| ------------ | ------------------------------------------------------------ |
| **`params`** | <code><a href="#record">Record</a>&lt;string, any&gt;</code> |

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
| **`projectid`**   | <code>string</code> |
| **`appid`**       | <code>string</code> |
| **`serviceName`** | <code>string</code> |


#### PushwooshTags


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

</docgen-api>
