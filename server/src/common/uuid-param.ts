import { NotFoundException, ParseUUIDPipe } from '@nestjs/common';

/**
 * Parses a UUID path parameter. Anything else is answered with the same 404 as an id that
 * does not exist, so malformed and foreign ids look alike.
 */
export function uuidOrNotFound(message: string): ParseUUIDPipe {
  return new ParseUUIDPipe({
    exceptionFactory: () => new NotFoundException(message, 'No encontrado'),
  });
}
