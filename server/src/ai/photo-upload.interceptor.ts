import {
  BadRequestException,
  CallHandler,
  ExecutionContext,
  HttpException,
  Injectable,
  NestInterceptor,
  PayloadTooLargeException,
} from '@nestjs/common';
import type { Request, Response } from 'express';
import multer from 'multer';
import type { Observable } from 'rxjs';

export const PHOTO_FIELD = 'foto';
export const MAX_PHOTO_BYTES = 2 * 1024 * 1024;

/**
 * Reads one photo from a multipart body into memory (never to disk), with tight limits:
 * one file of at most 2 MB in the `foto` field and a couple of short text fields. Multer's
 * own errors are replaced by safe Spanish ones. Guards run before this, so nothing is read
 * for a caller who may not use the endpoint.
 */
@Injectable()
export class PhotoUploadInterceptor implements NestInterceptor {
  private readonly upload = multer({
    storage: multer.memoryStorage(),
    limits: {
      fileSize: MAX_PHOTO_BYTES,
      files: 1,
      fields: 2,
      fieldSize: 64,
      parts: 3,
      headerPairs: 20,
    },
  }).single(PHOTO_FIELD);

  async intercept(
    context: ExecutionContext,
    next: CallHandler,
  ): Promise<Observable<unknown>> {
    const http = context.switchToHttp();
    await new Promise<void>((resolve, reject) => {
      this.upload(
        http.getRequest<Request>(),
        http.getResponse<Response>(),
        (error: unknown) =>
          error ? reject(toHttpException(error)) : resolve(),
      );
    });
    return next.handle();
  }
}

function toHttpException(error: unknown): HttpException {
  if (error instanceof multer.MulterError && error.code === 'LIMIT_FILE_SIZE') {
    return new PayloadTooLargeException(
      'La foto no puede pesar más de 2 MB.',
      'Archivo demasiado grande',
    );
  }
  // Unexpected fields, too many parts, a malformed body: never echo multer's text
  return new BadRequestException(
    'Los datos enviados no son válidos.',
    'Solicitud inválida',
  );
}
