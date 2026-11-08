import { OfferStatus } from '../generated/prisma/enums';
import { isOwnOfferPhotoUrl, offerPhotoFolderUrl } from './offer-image-url';
import {
  discountPercent,
  effectiveStatus,
  statusForStock,
} from './offer-rules';

describe('offer rules', () => {
  const now = new Date('2026-11-07T20:00:00Z');
  const past = new Date('2026-11-07T19:59:59Z');
  const future = new Date('2026-11-07T20:00:01Z');

  it.each([OfferStatus.AVAILABLE, OfferStatus.SOLD_OUT, OfferStatus.PAUSED])(
    'reads a %s offer as EXPIRED once its pickup ended',
    (status) => {
      expect(effectiveStatus(status, past, now)).toBe(OfferStatus.EXPIRED);
      expect(effectiveStatus(status, now, now)).toBe(OfferStatus.EXPIRED);
      expect(effectiveStatus(status, future, now)).toBe(status);
    },
  );

  it('keeps a cancelled offer cancelled, past or not', () => {
    expect(effectiveStatus(OfferStatus.CANCELLED, past, now)).toBe(
      OfferStatus.CANCELLED,
    );
  });

  it.each([
    [OfferStatus.AVAILABLE, 0, OfferStatus.SOLD_OUT],
    [OfferStatus.AVAILABLE, 2, OfferStatus.AVAILABLE],
    [OfferStatus.SOLD_OUT, 1, OfferStatus.AVAILABLE],
    [OfferStatus.SOLD_OUT, 0, OfferStatus.SOLD_OUT],
    [OfferStatus.PAUSED, 0, OfferStatus.PAUSED],
    [OfferStatus.PAUSED, 3, OfferStatus.PAUSED],
  ])(
    'after stock changes, %s with %d left becomes %s',
    (stored, left, expected) => {
      expect(statusForStock(stored, left)).toBe(expected);
    },
  );

  it.each([
    [12000, 6000, 50],
    [15000, 8900, 40],
    [100, 99, 1],
    [1000, 1000, 0],
    [0, 0, 0],
  ])('%d → %d is %d %% off, rounded down', (original, discounted, percent) => {
    expect(discountPercent(original, discounted)).toBe(percent);
  });

  describe('offer photo URLs', () => {
    const base = 'https://ref.supabase.co/';
    const uid = '3f1e8a2b-0c4d-4e5f-8a9b-0c1d2e3f4a5b';

    it('builds the folder of a user in the public bucket', () => {
      expect(offerPhotoFolderUrl(base, uid)).toBe(
        `https://ref.supabase.co/storage/v1/object/public/offer-photos/${uid}/`,
      );
    });

    it.each(['pan.jpg', 'a-b_C9.jpeg', 'x.webp'])(
      'accepts %s in the own folder',
      (file) => {
        expect(
          isOwnOfferPhotoUrl(offerPhotoFolderUrl(base, uid) + file, base, uid),
        ).toBe(true);
      },
    );

    it.each([
      `https://ref.supabase.co/storage/v1/object/public/offer-photos/otro-uid/pan.jpg`,
      `https://ref.supabase.co/storage/v1/object/public/otro-bucket/${uid}/pan.jpg`,
      `https://otro.supabase.co/storage/v1/object/public/offer-photos/${uid}/pan.jpg`,
      `http://ref.supabase.co/storage/v1/object/public/offer-photos/${uid}/pan.jpg`,
      `https://ref.supabase.co/storage/v1/object/public/offer-photos/${uid}/`,
      `https://ref.supabase.co/storage/v1/object/public/offer-photos/${uid}/a/pan.jpg`,
      `https://ref.supabase.co/storage/v1/object/public/offer-photos/${uid}/pan.jpg?token=1`,
      `https://ref.supabase.co/storage/v1/object/public/offer-photos/${uid}/pan.jpg#x`,
      `https://ref.supabase.co/storage/v1/object/public/offer-photos/${uid}/..%2Fpan.jpg`,
      `https://ref.supabase.co/storage/v1/object/public/offer-photos/${uid}/pan.png`,
    ])('refuses %s', (url) => {
      expect(isOwnOfferPhotoUrl(url, base, uid)).toBe(false);
    });
  });
});
