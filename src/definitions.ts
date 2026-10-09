export interface PushwooshConfig {
  appid: string,
  serviceName?: string
}

/**
 * One message of the Message Inbox, as both native platforms report it.
 *
 * A field the native message does not carry is omitted, not sent as an empty string or null —
 * read it with `?.` or `??`, not with a truthiness check on a guaranteed key.
 */
export type InboxNotification = {
  /** Inbox message identifier. The only guaranteed field; every mutator takes it. */
  code: string;
  title?: string;
  message?: string;
  /** Icon shown next to the message in the inbox list. */
  imageUrl?: string;
  /** Banner image of the message, taken from the `b` action parameter on both platforms. */
  bannerUrl?: string;
  /** ISO 8601 with a timezone offset, e.g. `2026-10-07T14:32:05+0300`. */
  sendDate?: string;
  /**
   * Message type, with the same codes on Android and iOS:
   * 0 plain, 1 rich media, 2 URL, 3 deep link.
   */
  type?: number;
  isRead?: boolean;
  isActionPerformed?: boolean;
  /** Raw action parameters of the message as sent by Pushwoosh. */
  actionParams?: Record<string, unknown>;
  /** Custom data attached to the push, taken from the `u` action parameter. */
  customData?: Record<string, unknown>;
};

/**
 * Appearance of the native inbox screen, applied per `presentInboxUI` call.
 *
 * Colors accept `#RGB`, `#RRGGBB`, `#AARRGGBB`, `0xRRGGBB` and `0xAARRGGBB`. The hex digits may be
 * upper or lower case; the `0x` prefix must be lowercase.
 * Images are addressed by a path relative to your app's `webDir`, so they can live next to the
 * rest of your web assets. A value the platform cannot parse is ignored with a console warning
 * and the remaining keys still apply.
 */
export interface InboxStyle {
  /** Date pattern of the message date. Android `SimpleDateFormat`, iOS `NSDateFormatter`. */
  dateFormat?: string;
  /** Title of the navigation bar. */
  barTitle?: string;
  /** Text shown when the inbox is empty. */
  listEmptyMessage?: string;
  /** Text shown when the inbox fails to load. */
  listErrorMessage?: string;
  /** Icon shown next to a message that carries no image of its own. */
  defaultImageIcon?: string;
  /** Image shown when the inbox is empty. */
  listEmptyImage?: string;
  /** Image shown when the inbox fails to load. */
  listErrorImage?: string;
  accentColor?: string;
  backgroundColor?: string;
  /** Color of the pressed state of a row. */
  highlightColor?: string;
  titleColor?: string;
  descriptionColor?: string;
  dateColor?: string;
  /** Color of the separator between rows. */
  dividerColor?: string;
  barBackgroundColor?: string;
  barAccentColor?: string;
  barTextColor?: string;
  /** Android only: title color of a read message. */
  readTitleColor?: string;
  /** Android only: text color of a read message. */
  readDescriptionColor?: string;
  /** Android only: date color of a read message. */
  readDateColor?: string;
  /** iOS only: the unread marker image. */
  unreadImage?: string;
  /** iOS only: the default text color. */
  defaultTextColor?: string;
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
  /**
   * Reads the device tags from Pushwoosh.
   *
   * Resolves to the tags as a flat object (tag name → value), with no `tags` envelope —
   * the same shape the native SDKs and the other Pushwoosh plugins return.
   * Rejects when the native read fails.
   */
  getTags(): Promise<PushwooshTags>;
  getPushToken(): Promise<{ value: string | null }>;
  getPushwooshHWID(): Promise<{ value: string | null }>;
  getRemoteNotificationStatus(): Promise<{ status: RemoteNotificationStatus }>;
  setApplicationIconBadgeNumber(badge: number): void;
  getApplicationIconBadgeNumber(): Promise<{ badge: number }>;
  addToApplicationIconBadgeNumber(badge: number): void;
  /**
   * Android resolves the launch notification as a JSON string under `value`, and resolves nothing
   * when the app was not opened from a push, so the promise settles as `undefined`. iOS resolves
   * the raw notification dictionary under `notification`, or `null` in that same case. The declared
   * shape below matches neither platform; the payload mismatch is tracked separately.
   */
  getLaunchNotification(): Promise<{ notification: string }>;
  clearLaunchNotification(): void;
  setUserId(userId: string): void;
  setLanguage(language: string): void;
  setApiToken(token: string): void;
  postEvent(event: string, attributes?: Record<string, string>): void;
  setMultiNotificationMode(): Promise<void>;
  setSingleNotificationMode(): Promise<void>;
  pushReceivedCallback(callback: PushwooshNotificationCallback): Promise<CallbackID>;
  pushOpenedCallback(callback: PushwooshNotificationCallback): Promise<CallbackID>;
  /**
   * Opens the native inbox screen of the Pushwoosh SDK.
   *
   * The style of each call is the SDK defaults plus the keys passed here — keys of a previous
   * call do not carry over. Resolves once the screen is on display, and rejects with the reason
   * in the message when it cannot be: no view controller or activity to present it from, or a
   * presentation of the inbox already under way.
   */
  presentInboxUI(style?: InboxStyle): Promise<void>;
  /** Reads the inbox from the server. An empty inbox resolves an empty array, not an error. */
  loadMessages(): Promise<{ messages: InboxNotification[] }>;
  /** Total number of inbox messages. */
  messagesCount(): Promise<{ value: number }>;
  /** Number of inbox messages the user has not read. */
  unreadMessagesCount(): Promise<{ value: number }>;
  /** Number of inbox messages whose action has not been performed. */
  messagesWithNoActionPerformedCount(): Promise<{ value: number }>;
  /** Marks one message as read. */
  readMessage(options: { code: string }): Promise<void>;
  /** Marks a batch of messages as read. */
  readMessages(options: { codes: string[] }): Promise<void>;
  /** Deletes one message from the inbox. */
  deleteMessage(options: { code: string }): Promise<void>;
  /** Deletes a batch of messages from the inbox. */
  deleteMessages(options: { codes: string[] }): Promise<void>;
  /** Performs the message's own action — opens its rich media, URL or deep link. */
  performAction(options: { code: string }): Promise<void>;
  setCommunicationEnabled(enabled: boolean): Promise<{ result: void | string }>;
  isCommunicationEnabled(success: (enabled: boolean) => void): void;
}
