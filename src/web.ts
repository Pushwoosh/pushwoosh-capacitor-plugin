import { WebPlugin } from '@capacitor/core';

import type { PushwooshPlugin } from './definitions';

export class PushwooshWeb extends WebPlugin implements PushwooshPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async registerDevice(success?: (pushToken: string) => void, fail?: (error: string) => void): void {
  }
}
