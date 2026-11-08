import { createHash } from 'node:crypto';
import { Injectable } from '@nestjs/common';
import { OfferCategory } from '../generated/prisma/enums';
import {
  OfferSuggester,
  SUGGESTION_RULES,
  type PhotoInput,
  type SuggestionDraft,
} from './offer-suggestion';

const SAMPLES: ReadonlyArray<
  Pick<SuggestionDraft, 'title' | 'category' | 'description'>
> = [
  {
    title: 'Bolsa sorpresa de panadería',
    category: OfferCategory.BAKERY,
    description:
      'Pan del día y piezas de hojaldre que no alcanzaron a venderse. Ideales para el desayuno o las onces.',
  },
  {
    title: 'Almuerzo del día para llevar',
    category: OfferCategory.PREPARED_FOOD,
    description:
      'Porción del menú de hoy, preparada esta mañana. Recógela en el horario indicado y caliéntala antes de comer.',
  },
  {
    title: 'Caja de postres surtidos',
    category: OfferCategory.DESSERTS,
    description:
      'Selección de postres de la vitrina de hoy. Mantenlos refrigerados y consúmelos el mismo día.',
  },
  {
    title: 'Canasta de frutas y verduras',
    category: OfferCategory.PRODUCE,
    description:
      'Frutas y verduras frescas con detalles de apariencia, perfectas para jugos, sopas o compotas.',
  },
  {
    title: 'Lácteos con fecha próxima',
    category: OfferCategory.DAIRY,
    description:
      'Productos lácteos en perfecto estado con fecha de consumo cercana. Mantener en refrigeración.',
  },
  {
    title: 'Bolsa sorpresa del día',
    category: OfferCategory.OTHER,
    description:
      'Productos variados del día que siguen en buen estado. El contenido cambia según lo que quede en la tienda.',
  },
];

const DISCOUNT_STEP = 5;

/**
 * Stands in for the model when no API key is configured: a fixed set of plausible offers,
 * chosen from a hash of the photo, so the same photo always gets the same suggestion and
 * nothing leaves the server.
 */
@Injectable()
export class MockOfferSuggester extends OfferSuggester {
  readonly source = 'mock' as const;

  suggest(photo: PhotoInput): Promise<SuggestionDraft> {
    const digest = createHash('sha256').update(photo.bytes).digest();
    const sample = SAMPLES[digest[0] % SAMPLES.length];
    const steps =
      (SUGGESTION_RULES.maxDiscountPercent -
        SUGGESTION_RULES.minDiscountPercent) /
      DISCOUNT_STEP;
    const discountPercent =
      SUGGESTION_RULES.minDiscountPercent +
      (digest[1] % (steps + 1)) * DISCOUNT_STEP;

    return Promise.resolve({
      ...sample,
      discountPercent,
      confidence: 0.5,
      isFood: true,
      warnings: [
        'Sugerencia de ejemplo: el análisis automático de fotos no está activo. Revisa y ajusta los datos antes de publicar.',
      ],
    });
  }
}
