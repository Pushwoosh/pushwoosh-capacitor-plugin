import { registerPlugin } from '@capacitor/core';

import type { PushwooshPlugin, PushwooshNotificationCallback } from './definitions';

const PushwooshNative = registerPlugin<any>('Pushwoosh', {
  web: () => import('./web').then((m) => new m.PushwooshWeb()),
});

// Create a wrapper that transforms primitive parameters to object parameters
const Pushwoosh: PushwooshPlugin = {
  onDeviceReady: PushwooshNative.onDeviceReady.bind(PushwooshNative),
  registerDevice: PushwooshNative.registerDevice.bind(PushwooshNative),
  unregisterDevice: PushwooshNative.unregisterDevice.bind(PushwooshNative),
  additionalAuthorizationOptions: PushwooshNative.additionalAuthorizationOptions.bind(PushwooshNative),
  setTags: PushwooshNative.setTags.bind(PushwooshNative),
  getTags: PushwooshNative.getTags.bind(PushwooshNative),
  getPushToken: PushwooshNative.getPushToken.bind(PushwooshNative),
  getPushwooshHWID: PushwooshNative.getPushwooshHWID.bind(PushwooshNative),
  getRemoteNotificationStatus: PushwooshNative.getRemoteNotificationStatus.bind(PushwooshNative),
  getLaunchNotification: PushwooshNative.getLaunchNotification.bind(PushwooshNative),
  clearLaunchNotification: PushwooshNative.clearLaunchNotification.bind(PushwooshNative),
  setMultiNotificationMode: PushwooshNative.setMultiNotificationMode.bind(PushwooshNative),
  setSingleNotificationMode: PushwooshNative.setSingleNotificationMode.bind(PushwooshNative),
  // Capacitor's native bridge warns when the handler arrives as `options`; it belongs in the callback slot.
  pushReceivedCallback: (callback: PushwooshNotificationCallback) =>
    PushwooshNative.pushReceivedCallback({}, callback),
  pushOpenedCallback: (callback: PushwooshNotificationCallback) =>
    PushwooshNative.pushOpenedCallback({}, callback),
  presentInboxUI: PushwooshNative.presentInboxUI.bind(PushwooshNative),
  loadMessages: PushwooshNative.loadMessages.bind(PushwooshNative),
  messagesCount: PushwooshNative.messagesCount.bind(PushwooshNative),
  unreadMessagesCount: PushwooshNative.unreadMessagesCount.bind(PushwooshNative),
  messagesWithNoActionPerformedCount: PushwooshNative.messagesWithNoActionPerformedCount.bind(PushwooshNative),
  readMessage: PushwooshNative.readMessage.bind(PushwooshNative),
  readMessages: PushwooshNative.readMessages.bind(PushwooshNative),
  deleteMessage: PushwooshNative.deleteMessage.bind(PushwooshNative),
  deleteMessages: PushwooshNative.deleteMessages.bind(PushwooshNative),
  performAction: PushwooshNative.performAction.bind(PushwooshNative),
  isCommunicationEnabled: PushwooshNative.isCommunicationEnabled.bind(PushwooshNative),

  // Wrapped methods that transform primitive parameters to objects
  setUserId: (userId: string) => {
    return PushwooshNative.setUserId({ userId });
  },

  setLanguage: (language: string) => {
    return PushwooshNative.setLanguage({ language });
  },

  setApiToken: (token: string) => {
    return PushwooshNative.setApiToken({ token });
  },

  postEvent: (event: string, attributes?: Record<string, string>) => {
    return PushwooshNative.postEvent({ event, attributes });
  },

  setApplicationIconBadgeNumber: (badge: number) => {
    return PushwooshNative.setApplicationIconBadgeNumber({ badge });
  },

  getApplicationIconBadgeNumber: () => {
    return PushwooshNative.getApplicationIconBadgeNumber();
  },

  addToApplicationIconBadgeNumber: (badge: number) => {
    return PushwooshNative.addToApplicationIconBadgeNumber({ badge });
  },

  setCommunicationEnabled: (enabled: boolean) => {
    return PushwooshNative.setCommunicationEnabled({ enabled });
  },
};

export * from './definitions';
export { Pushwoosh };
