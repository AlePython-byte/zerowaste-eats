import type { SuggestionQuotaService } from '../../src/ai/suggestion-quota.service';
import { authenticatedUser } from './user-profile';

export const USER_ID = authenticatedUser.id;

export const RESERVATION = {
  merchantProfileId: '5d0c7a3e-3f43-4c58-9a57-1f1f3f0b7c11',
  day: '2026-11-07',
};

/** A quota that always has room unless a test says otherwise. */
export const reserve = jest.fn();
export const release = jest.fn();
export const quota = { reserve, release } as unknown as SuggestionQuotaService;

export function resetQuota(): void {
  reserve.mockReset().mockResolvedValue(RESERVATION);
  release.mockReset().mockResolvedValue(undefined);
}
