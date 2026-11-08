/**
 * The Supabase Storage bucket for offer photos. Its policies, in
 * docs/supabase/storage-offer-photos.sql, only let a user write in their own folder.
 */
export const OFFER_PHOTOS_BUCKET = 'offer-photos';

/** File names the app writes: letters, digits, dashes and underscores, then a photo extension. */
const FILE_NAME = /^[A-Za-z0-9_-]{1,100}\.(jpg|jpeg|webp)$/;

/** The public URL prefix of a user's folder: `<project>/storage/v1/object/public/offer-photos/<uid>/`. */
export function offerPhotoFolderUrl(
  supabaseUrl: string,
  userId: string,
): string {
  return `${supabaseUrl.replace(/\/+$/, '')}/storage/v1/object/public/${OFFER_PHOTOS_BUCKET}/${userId}/`;
}

/**
 * True only for a photo in the business's own folder of the bucket: exactly the folder
 * prefix followed by a plain file name, with no subfolders, query, fragment or encoded
 * characters. Anything else could point at another business's photo or another site.
 */
export function isOwnOfferPhotoUrl(
  url: string,
  supabaseUrl: string,
  userId: string,
): boolean {
  const prefix = offerPhotoFolderUrl(supabaseUrl, userId);
  if (!url.startsWith(prefix)) return false;
  return FILE_NAME.test(url.slice(prefix.length));
}
