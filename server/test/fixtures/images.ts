/** Minimal byte prefixes of real formats. Only the signature matters to the server. */
export const jpegBytes = (size = 256, seed = 0): Buffer => {
  const bytes = Buffer.alloc(size, seed);
  bytes.set([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46]);
  return bytes;
};

export const webpBytes = (size = 256): Buffer => {
  const bytes = Buffer.alloc(size, 0);
  bytes.write('RIFF', 0, 'latin1');
  bytes.writeUInt32LE(size - 8, 4);
  bytes.write('WEBPVP8 ', 8, 'latin1');
  return bytes;
};

export const pngBytes = (size = 256): Buffer => {
  const bytes = Buffer.alloc(size, 0);
  bytes.set([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  return bytes;
};

export const gifBytes = (): Buffer => Buffer.from('GIF89a......', 'latin1');
