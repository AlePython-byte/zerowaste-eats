export type SupportedImageType = 'image/jpeg' | 'image/webp';

/**
 * The real type of an uploaded photo, read from its first bytes. The file name and the
 * declared content type come from the client and prove nothing, so they are ignored.
 * Returns null for anything that is not JPEG or WebP.
 */
export function detectImageType(bytes: Buffer): SupportedImageType | null {
  // JPEG: starts with the SOI marker FF D8 followed by another marker (FF ..)
  if (
    bytes.length >= 3 &&
    bytes[0] === 0xff &&
    bytes[1] === 0xd8 &&
    bytes[2] === 0xff
  ) {
    return 'image/jpeg';
  }
  // WebP: a RIFF container whose form type is WEBP
  if (
    bytes.length >= 12 &&
    bytes.toString('latin1', 0, 4) === 'RIFF' &&
    bytes.toString('latin1', 8, 12) === 'WEBP'
  ) {
    return 'image/webp';
  }
  return null;
}
