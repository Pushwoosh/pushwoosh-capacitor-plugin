import { registerPlugin } from '@capacitor/core';

import type { PushwooshPlugin } from './definitions';

const Pushwoosh = registerPlugin<PushwooshPlugin>('Pushwoosh', {
  web: () => import('./web').then((m) => new m.PushwooshWeb()),
});

export * from './definitions';
export { Pushwoosh };
