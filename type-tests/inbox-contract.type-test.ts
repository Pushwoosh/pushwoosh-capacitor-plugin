/**
 * Type-level contract guard for the Message Inbox API.
 *
 * Every method of the Capacitor plugin proxy resolves through a promise, and the native `resolve`
 * only accepts an object — so counters resolve `{ value }`, the list resolves `{ messages }`, and
 * the mutators take their arguments as one object. A declaration that drifts from any of those
 * makes the example in the README stop compiling for a host that trusts the shipped `.d.ts`.
 *
 * The negative cases stop erroring the moment a shape goes back to the old, looser declaration —
 * then `tsc` reports the unused `@ts-expect-error` directives and this file fails to compile.
 *
 * Run: `npm run test:types`
 */
import { Pushwoosh } from '../src/index';
import type { InboxNotification, InboxStyle } from '../src/index';

export function presentInboxUIIsAsyncAndTakesAStyle(): Promise<void> {
  const style: InboxStyle = { barTitle: 'Demo Inbox', dateFormat: 'dd.MM.yyyy', accentColor: '#80ff6600' };
  return Pushwoosh.presentInboxUI(style);
}

export function presentInboxUIStyleIsOptional(): Promise<void> {
  return Pushwoosh.presentInboxUI();
}

export function presentInboxUIRejectsUnknownStyleKey(): Promise<void> {
  // @ts-expect-error a typo in a style key is a compile error, not a silently ignored key
  return Pushwoosh.presentInboxUI({ barTitel: 'Demo Inbox' });
}

export async function loadMessagesResolvesTypedMessages(): Promise<InboxNotification[]> {
  const result = await Pushwoosh.loadMessages();
  return result.messages;
}

export async function inboxMessageFieldsAreTyped(): Promise<string> {
  const [message] = (await Pushwoosh.loadMessages()).messages;
  const read = message.isRead ?? false;
  const custom = message.customData?.orderId;
  return `${message.code} ${message.title ?? ''} ${read} ${String(custom)}`;
}

export function loadMessagesRejectsBareArray(): Promise<InboxNotification[]> {
  // @ts-expect-error the list is wrapped in a `messages` envelope, as the native resolve requires
  return Pushwoosh.loadMessages();
}

export function countersResolveValue(): Promise<{ value: number }>[] {
  return [
    Pushwoosh.messagesCount(),
    Pushwoosh.unreadMessagesCount(),
    Pushwoosh.messagesWithNoActionPerformedCount(),
  ];
}

export async function countersAwaitToNumber(): Promise<number> {
  const total = await Pushwoosh.messagesCount();
  const unread = await Pushwoosh.unreadMessagesCount();
  return total.value - unread.value;
}

export function countersRejectSyncMember(): number {
  // @ts-expect-error `value` is read off the promise itself, which has no such member
  return Pushwoosh.unreadMessagesCount().value;
}

export function singleCodeMutatorsTakeAnObject(): Promise<void>[] {
  return [
    Pushwoosh.readMessage({ code: 'abc' }),
    Pushwoosh.deleteMessage({ code: 'abc' }),
    Pushwoosh.performAction({ code: 'abc' }),
  ];
}

export function singleCodeMutatorsRejectABareString(): Promise<void> {
  // @ts-expect-error arguments arrive as one object, never as a positional primitive
  return Pushwoosh.readMessage('abc');
}

export function batchMutatorsTakeACodesArray(): Promise<void>[] {
  return [Pushwoosh.readMessages({ codes: ['a', 'b'] }), Pushwoosh.deleteMessages({ codes: ['a', 'b'] })];
}

export function batchMutatorsRejectASingleCode(): Promise<void> {
  // @ts-expect-error the batch methods take `codes`, not `code`
  return Pushwoosh.deleteMessages({ code: 'a' });
}
