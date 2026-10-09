/**
 * Type-level regression guard: the getters of the plugin facade must stay Promise-typed.
 *
 * Every method of the Capacitor plugin proxy resolves through a promise, so a declaration
 * that promises a plain object makes a host written against the shipped `.d.ts` dereference
 * the promise itself and throw synchronously in its own frame (`SyntaxError`/`TypeError`,
 * killing the rest of that host function).
 *
 * The negative cases below stop erroring the moment a getter goes back to a synchronous
 * declaration — then `tsc` reports the unused `@ts-expect-error` directives and this file
 * fails to compile.
 *
 * For every getter the suite pins awaitability; for `getPushToken` and `getPushwooshHWID` it
 * also pins the `value` field, which both native sides really resolve, and for `getTags` the
 * flat result (tags at the top level, no `tags` envelope), which is what both native sides
 * resolve and what the other Pushwoosh plugins return. Only `getLaunchNotification`'s payload
 * shape is left unpinned — it differs per platform and is tracked separately.
 *
 * Run: `npm run test:types`
 */
import { Pushwoosh } from '../src/index';
import type { PushwooshTags } from '../src/index';

export function launchNotificationIsAsync(): Promise<{ notification: string }> {
  return Pushwoosh.getLaunchNotification();
}

export function pushTokenIsAsync(): Promise<{ value: string | null }> {
  return Pushwoosh.getPushToken();
}

export function hwidIsAsync(): Promise<{ value: string | null }> {
  return Pushwoosh.getPushwooshHWID();
}

export async function pushTokenAwaitsToNullableString(): Promise<string> {
  const token = await Pushwoosh.getPushToken();
  return token.value ?? 'not registered';
}

export async function hwidAwaitsToNullableString(): Promise<string> {
  const hwid = await Pushwoosh.getPushwooshHWID();
  return hwid.value ?? 'unknown';
}

export function launchNotificationRejectsSyncParse(): unknown {
  // @ts-expect-error a host that trusts a synchronous declaration parses the promise itself
  return JSON.parse(Pushwoosh.getLaunchNotification().notification);
}

export function launchNotificationRejectsSyncMember(): number {
  // @ts-expect-error same lie, read as a member instead of parsed
  return Pushwoosh.getLaunchNotification().notification.length;
}

export function pushTokenRejectsSyncMember(): number {
  // @ts-expect-error `value` is read off the promise itself, which has no such member
  return Pushwoosh.getPushToken().value!.length; // eslint-disable-line @typescript-eslint/no-non-null-assertion
}

export function hwidRejectsSyncMember(): string {
  // @ts-expect-error a nullish-coalescing guard does not make the synchronous read legal
  return Pushwoosh.getPushwooshHWID().value ?? 'unknown';
}

export function tagsAreFlat(): Promise<PushwooshTags> {
  return Pushwoosh.getTags();
}

export async function tagsIndexByStringKey(): Promise<string | number | string[] | number[] | undefined> {
  const tags = await Pushwoosh.getTags();
  return tags['favorite_categories'];
}

export async function legacyTagsDestructuringStillCompiles(): Promise<unknown> {
  // Hosts written against the old `{ tags }` declaration keep compiling; they read undefined, as they already did at runtime.
  const { tags } = await Pushwoosh.getTags();
  return tags;
}

export function tagsRejectEnvelopeShape(): Promise<{ tags: PushwooshTags }> {
  // @ts-expect-error the declaration no longer promises a `tags` envelope
  return Pushwoosh.getTags();
}
