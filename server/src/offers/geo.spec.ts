import { boundingBox, haversineKm } from './geo';

const PASTO = { latitude: 1.213611, longitude: -77.281111 };
/** About 1 km north of downtown Pasto. */
const ONE_KM_NORTH = {
  latitude: 1.213611 + 1 / 111.195,
  longitude: -77.281111,
};

describe('haversineKm', () => {
  it('is zero for the same point', () => {
    expect(haversineKm(PASTO, PASTO)).toBe(0);
  });

  it('measures a degree of latitude as about 111.2 km', () => {
    expect(
      haversineKm({ latitude: 0, longitude: 0 }, { latitude: 1, longitude: 0 }),
    ).toBeCloseTo(111.195, 2);
  });

  it('measures short city distances accurately', () => {
    expect(haversineKm(PASTO, ONE_KM_NORTH)).toBeCloseTo(1, 2);
  });

  it('matches a known long distance: Pasto to Bogotá is about 520 km', () => {
    const bogota = { latitude: 4.711, longitude: -74.0721 };

    expect(haversineKm(PASTO, bogota)).toBeGreaterThan(510);
    expect(haversineKm(PASTO, bogota)).toBeLessThan(530);
  });

  it('is symmetric', () => {
    const ipiales = { latitude: 0.8302, longitude: -77.6444 };

    expect(haversineKm(PASTO, ipiales)).toBeCloseTo(
      haversineKm(ipiales, PASTO),
      10,
    );
  });
});

describe('boundingBox', () => {
  it('contains every point on the radius, in all directions', () => {
    const box = boundingBox(PASTO, 5);
    for (let bearing = 0; bearing < 360; bearing += 15) {
      // A point just inside 5 km in this direction (flat approximation is fine at this scale)
      const rad = (bearing * Math.PI) / 180;
      const point = {
        latitude: PASTO.latitude + (4.99 / 111.195) * Math.cos(rad),
        longitude:
          PASTO.longitude +
          (4.99 / (111.195 * Math.cos((PASTO.latitude * Math.PI) / 180))) *
            Math.sin(rad),
      };
      expect(haversineKm(PASTO, point)).toBeLessThanOrEqual(5);
      expect(point.latitude).toBeGreaterThanOrEqual(box.minLatitude - 1e-9);
      expect(point.latitude).toBeLessThanOrEqual(box.maxLatitude + 1e-9);
      expect(point.longitude).toBeGreaterThanOrEqual(box.minLongitude! - 1e-9);
      expect(point.longitude).toBeLessThanOrEqual(box.maxLongitude! + 1e-9);
    }
  });

  it('is about 0.045° wide in latitude for 5 km', () => {
    const box = boundingBox(PASTO, 5);

    expect(box.maxLatitude - box.minLatitude).toBeCloseTo(0.0899, 3);
  });

  it('widens in longitude away from the equator', () => {
    const atEquator = boundingBox({ latitude: 0, longitude: 0 }, 10);
    const north = boundingBox({ latitude: 60, longitude: 0 }, 10);

    expect(north.maxLongitude! - north.minLongitude!).toBeCloseTo(
      2 * (atEquator.maxLongitude! - atEquator.minLongitude!),
      3,
    );
  });

  it('skips the longitude filter when the box would wrap past 180°, and near the poles', () => {
    expect(
      boundingBox({ latitude: 0, longitude: 179.99 }, 50).minLongitude,
    ).toBeNull();
    expect(
      boundingBox({ latitude: 90, longitude: 0 }, 5).minLongitude,
    ).toBeNull();
    expect(boundingBox({ latitude: 89.99, longitude: 0 }, 5).maxLatitude).toBe(
      90,
    );
  });
});
