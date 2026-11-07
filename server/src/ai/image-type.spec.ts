import {
  gifBytes,
  jpegBytes,
  pngBytes,
  webpBytes,
} from '../../test/fixtures/images';
import { detectImageType } from './image-type';

describe('detectImageType', () => {
  it('recognizes JPEG and WebP by their bytes', () => {
    expect(detectImageType(jpegBytes())).toBe('image/jpeg');
    expect(detectImageType(webpBytes())).toBe('image/webp');
  });

  it.each([
    ['PNG', pngBytes()],
    ['GIF', gifBytes()],
    ['plain text', Buffer.from('not an image at all')],
    ['an empty file', Buffer.alloc(0)],
    ['a truncated JPEG marker', Buffer.from([0xff, 0xd8])],
    [
      'a RIFF file that is not WebP',
      Buffer.from('RIFF\0\0\0\0WAVEfmt ', 'latin1'),
    ],
  ])('rejects %s', (_label, bytes) => {
    expect(detectImageType(bytes)).toBeNull();
  });
});
