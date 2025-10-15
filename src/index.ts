import { registerPlugin } from '@capacitor/core';

import type { PushwooshPlugin } from './definitions';

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
  pushReceivedCallback: PushwooshNative.pushReceivedCallback.bind(PushwooshNative),
  pushOpenedCallback: PushwooshNative.pushOpenedCallback.bind(PushwooshNative),
  presentInboxUI: PushwooshNative.presentInboxUI.bind(PushwooshNative),
  isCommunicationEnabled: PushwooshNative.isCommunicationEnabled.bind(PushwooshNative),

  // Wrapped methods that transform primitive parameters to objects
  setUserId: (userId: string) => {
    return PushwooshNative.setUserId({ userId });
  },

  setLanguage: (language: string) => {
    return PushwooshNative.setLanguage({ language });
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
