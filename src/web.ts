import { WebPlugin } from '@capacitor/core';
import {PushwooshConfig, PushwooshTags, AuthOptions, RemoteNotificationStatus, PushwooshNotificationCallback, CallbackID } from './definitions';
import type { PushwooshPlugin, InboxNotification, InboxStyle } from './definitions';

export class PushwooshWeb extends WebPlugin implements PushwooshPlugin {

  // onDeviceReady implementation
  onDeviceReady(config: PushwooshConfig): void {
    console.log("onDeviceReady called with config:", config);
  }

  // registerDevice implementation
  async registerDevice(): Promise<{ pushToken: string }> {
    console.log("registerDevice called");
    return { pushToken: "dummyPushToken" }; // return a dummy token for now
  }

  // unregisterDevice implementation
  async unregisterDevice(): Promise<void> {
    console.log("unregisterDevice called");
  }

  // additionalAuthorizationOptions implementation
  additionalAuthorizationOptions(options: AuthOptions): void {
    console.log("additionalAuthorizationOptions called with options:", options);
  }

  // setTags implementation
  async setTags(config: PushwooshTags): Promise<void> {
    console.log("setTags called with config:", config);
  }

  // getTags implementation
  async getTags(): Promise<PushwooshTags> {
    console.log("getTags called");
    return {}; // return empty tags as a stub
  }

  // getPushToken implementation
  async getPushToken(): Promise<{ value: string | null }> {
    console.log("getPushToken called");
    return { value: "dummyPushToken" }; // return a dummy token
  }

  // getPushwooshHWID implementation
  async getPushwooshHWID(): Promise<{ value: string | null }> {
    console.log("getPushwooshHWID called");
    return { value: "dummyHWID" }; // return a dummy HWID
  }

  // getRemoteNotificationStatus implementation
  async getRemoteNotificationStatus(): Promise<{ status: RemoteNotificationStatus }> {
    console.log("getRemoteNotificationStatus called");
    return { status: { code: "enabled" } }; 
  }

  // setApplicationIconBadgeNumber implementation
  setApplicationIconBadgeNumber(badge: number): void {
    console.log("setApplicationIconBadgeNumber called with badge:", badge);
  }

  // getApplicationIconBadgeNumber implementation
  async getApplicationIconBadgeNumber(): Promise<{ badge: number }> {
    console.log("getApplicationIconBadgeNumber called");
    return { badge: 0 }; // return a dummy badge number
  }

  // addToApplicationIconBadgeNumber implementation
  addToApplicationIconBadgeNumber(badge: number): void {
    console.log("addToApplicationIconBadgeNumber called with badge:", badge);
  }

  // getLaunchNotification implementation
  async getLaunchNotification(): Promise<{ notification: string }> {
    console.log("getLaunchNotification called");
    return { notification: "" }; // return empty notification as a stub
  }

  // clearLaunchNotification implementation
  clearLaunchNotification(): void {
    console.log("clearLaunchNotification called");
  }

  // setUserId implementation
  setUserId(userId: string): void {
    console.log("setUserId called with userId:", userId);
  }

  // setLanguage implementation
  setLanguage(language: string): void {
    console.log("setLanguage called with language:", language);
  }

  // setApiToken implementation
  setApiToken(token: string): void {
    console.log("setApiToken called with token:", token);
  }

  // postEvent implementation
  postEvent(event: string, attributes?: Record<string, string>): void {
    console.log("postEvent called with event:", event, "and attributes:", attributes);
  }

  // setMultiNotificationMode implementation
  async setMultiNotificationMode(): Promise<void> {
    console.log("setMultiNotificationMode called");
  }

  // setSingleNotificationMode implementation
  async setSingleNotificationMode(): Promise<void> {
    console.log("setSingleNotificationMode called");
  }

  // pushReceivedCallback implementation
  async pushReceivedCallback(callback: PushwooshNotificationCallback): Promise<CallbackID> {
    console.log("pushReceivedCallback called with callback:", callback);
    return "callbackID"; // return a dummy callback ID
  }

  // pushOpenedCallback implementation
  async pushOpenedCallback(callback: PushwooshNotificationCallback): Promise<CallbackID> {
    console.log("pushOpenedCallback called with callback:", callback);
    return "callbackID"; // return a dummy callback ID
  }

  // presentInboxUI implementation
  async presentInboxUI(style?: InboxStyle): Promise<void> {
    console.log('presentInboxUI called with style:', style);
  }

  // loadMessages implementation
  async loadMessages(): Promise<{ messages: InboxNotification[] }> {
    console.log('loadMessages called');
    return { messages: [] };
  }

  // messagesCount implementation
  async messagesCount(): Promise<{ value: number }> {
    console.log('messagesCount called');
    return { value: 0 };
  }

  // unreadMessagesCount implementation
  async unreadMessagesCount(): Promise<{ value: number }> {
    console.log('unreadMessagesCount called');
    return { value: 0 };
  }

  // messagesWithNoActionPerformedCount implementation
  async messagesWithNoActionPerformedCount(): Promise<{ value: number }> {
    console.log('messagesWithNoActionPerformedCount called');
    return { value: 0 };
  }

  // readMessage implementation
  async readMessage(options: { code: string }): Promise<void> {
    console.log('readMessage called with options:', options);
  }

  // readMessages implementation
  async readMessages(options: { codes: string[] }): Promise<void> {
    console.log('readMessages called with options:', options);
  }

  // deleteMessage implementation
  async deleteMessage(options: { code: string }): Promise<void> {
    console.log('deleteMessage called with options:', options);
  }

  // deleteMessages implementation
  async deleteMessages(options: { codes: string[] }): Promise<void> {
    console.log('deleteMessages called with options:', options);
  }

  // performAction implementation
  async performAction(options: { code: string }): Promise<void> {
    console.log('performAction called with options:', options);
  }

  // setCommunicationEnabled implementation
  async setCommunicationEnabled(enabled: boolean): Promise<{ result: void | string }> {
    console.log("setCommunicationEnabled called with enabled:", enabled);
    return { result: undefined }; // return a dummy result
  }

  // isCommunicationEnabled implementation
  isCommunicationEnabled(success: (enabled: boolean) => void): void {
    console.log("isCommunicationEnabled called");
    success(true); // call the success callback with a dummy value
  }
}
