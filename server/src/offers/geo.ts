/** Mean Earth radius in kilometres, the usual value for Haversine. */
export const EARTH_RADIUS_KM = 6371.0088;

/** Kilometres per degree of latitude (and of longitude at the equator). */
const KM_PER_DEGREE = (Math.PI * EARTH_RADIUS_KM) / 180;

export interface Point {
  latitude: number;
  longitude: number;
}

const radians = (degrees: number) => (degrees * Math.PI) / 180;

/** Great-circle distance between two points, in kilometres. */
export function haversineKm(a: Point, b: Point): number {
  const dLat = radians(b.latitude - a.latitude);
  const dLng = radians(b.longitude - a.longitude);
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(radians(a.latitude)) *
      Math.cos(radians(b.latitude)) *
      Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
}

export interface BoundingBox {
  minLatitude: number;
  maxLatitude: number;
  /** Null when the box would wrap around the 180° meridian: then longitude is not prefiltered. */
  minLongitude: number | null;
  maxLongitude: number | null;
}

/**
 * The latitude/longitude rectangle that contains every point within [radiusKm] of [center].
 * Used to let the database discard far establishments cheaply before the exact Haversine
 * distance is computed; it may let a few corner points through, never leave one out.
 */
export function boundingBox(center: Point, radiusKm: number): BoundingBox {
  const dLat = radiusKm / KM_PER_DEGREE;
  // Longitude degrees shrink with latitude; near the poles the box spans every longitude
  const cosLat = Math.cos(radians(center.latitude));
  const dLng = cosLat < 1e-6 ? 360 : radiusKm / (KM_PER_DEGREE * cosLat);
  const minLongitude = center.longitude - dLng;
  const maxLongitude = center.longitude + dLng;
  const wraps = minLongitude < -180 || maxLongitude > 180;
  return {
    minLatitude: Math.max(-90, center.latitude - dLat),
    maxLatitude: Math.min(90, center.latitude + dLat),
    minLongitude: wraps ? null : minLongitude,
    maxLongitude: wraps ? null : maxLongitude,
  };
}
