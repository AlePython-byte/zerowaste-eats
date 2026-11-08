import { randomInt } from 'node:crypto';

/**
 * Letters and digits a person can read aloud and type without doubt: no 0/O and no 1/I.
 * 32 symbols, so four of them give about a million codes.
 */
export const PICKUP_CODE_ALPHABET = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
export const PICKUP_CODE_LENGTH = 4;
const PREFIX = 'ZW';

/** A fresh pickup code such as ZW-7KQ3, from a cryptographically secure source. */
export function randomPickupCode(): string {
  let body = '';
  for (let i = 0; i < PICKUP_CODE_LENGTH; i++) {
    body += PICKUP_CODE_ALPHABET[randomInt(PICKUP_CODE_ALPHABET.length)];
  }
  return `${PREFIX}-${body}`;
}
