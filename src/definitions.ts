export interface PushwooshConfig {
  projectid: string,
  appid: string,
  serviceName?: string
}

export type InboxNotification = {
  code: string;
  title?: string;
  message?: string;
  imageUrl?: string;
  sendDate?: string;
  type?: number;
  bannerUrl?: string;
  customData?: Object;
  isRead?: boolean;
  actionParams?: Object;
  isActionPerformed?: boolean;
}

export type AuthOptions = Record<string, number|string>

export type RemoteNotificationStatus = Record<string,string|number|boolean>

export interface PushwooshTags {
  [index: string]: string | number | string[] | number[]
}

export type CallbackID = string;
export type PushwooshNotificationCallback = (message: string | null, err?: any) => void;

export interface PushwooshPlugin {
  onDeviceReady(config: PushwooshConfig): void;
  registerDevice(): Promise<{ pushToken: string }>;
  unregisterDevice(): Promise<void>;
  additionalAuthorizationOptions(options: AuthOptions): void;
  setTags(config: PushwooshTags) : Promise<void>;
  getTags(): Promise<{ tags: PushwooshTags }>;
  getPushToken(): { value: string | null };
  getPushwooshHWID(): { value: string | null };
  getRemoteNotificationStatus(): Promise<{ status: RemoteNotificationStatus }>;
  setApplicationIconBadgeNumber(badge: number): void;
  getApplicationIconBadgeNumber(): Promise<{ badge: number }>;
  addToApplicationIconBadgeNumber(badge: number): void;
  getLaunchNotification(): { notification: string } ;
  clearLaunchNotification(): void;
  setUserId(userId: string): void;
  setLanguage(language: string): void;
  postEvent(event: string, attributes?: Record<string, string>): void;
  setMultiNotificationMode(): Promise<void>;
  setSingleNotificationMode(): Promise<void>;
  pushReceivedCallback(callback: PushwooshNotificationCallback): Promise<CallbackID>;
  pushOpenedCallback(callback: PushwooshNotificationCallback): Promise<CallbackID>;
  presentInboxUI(params?: Record<string,any>): void;
  showGDPRConsentUI(): void;
  showGDPRDeletionUI(): void;
  setCommunicationEnabled(enabled: boolean): Promise<{ result: void | string }>;
  removeAllDeviceData(): Promise<{ result: void | string }>;
  isCommunicationEnabled(success: (enabled: boolean) => void): void;
  isDeviceDataRemoved(success: (removed: boolean) => void): void;
  isAvailableGDPR(success: (isAvailable: boolean) => void): void;
}
