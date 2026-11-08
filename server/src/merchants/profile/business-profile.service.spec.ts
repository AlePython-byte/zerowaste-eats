import {
  addBusiness,
  createFakeOffersDb,
} from '../../../test/fixtures/fake-offers-db';
import { BusinessProfileService } from './business-profile.service';

describe('BusinessProfileService', () => {
  let db: ReturnType<typeof createFakeOffersDb>;
  let service: BusinessProfileService;
  let mine: ReturnType<typeof addBusiness>;
  let theirs: ReturnType<typeof addBusiness>;

  beforeEach(() => {
    db = createFakeOffersDb();
    mine = addBusiness(db.tables, { businessName: 'Mi panadería' });
    theirs = addBusiness(db.tables, { businessName: 'La de enfrente' });
    service = new BusinessProfileService(db.service);
  });

  it('shows the caller business with its establishment and location', async () => {
    await expect(service.get(mine.userId)).resolves.toEqual({
      id: mine.profile.id,
      businessName: 'Mi panadería',
      description: null,
      isVerified: false,
      establishment: {
        id: mine.establishment.id,
        name: 'Mi panadería',
        address: 'Centro de Pasto',
        city: 'Pasto',
        latitude: 1.213611,
        longitude: -77.281111,
      },
    });
  });

  it('changes the name, description and location of the caller business only', async () => {
    const updated = await service.update(mine.userId, {
      businessName: '  Panadería La Esquina  ',
      description: 'Pan artesanal desde 1998.',
      establishment: {
        address: 'Calle 18 # 25-30',
        latitude: 1.2141234567,
        longitude: -77.2783219876,
      },
    });

    expect(updated).toMatchObject({
      businessName: 'Panadería La Esquina',
      description: 'Pan artesanal desde 1998.',
      establishment: {
        name: 'Mi panadería',
        address: 'Calle 18 # 25-30',
        city: 'Pasto',
        latitude: 1.214123,
        longitude: -77.278322,
      },
    });
    // The other business is untouched
    expect(db.tables.merchantProfiles[1].businessName).toBe('La de enfrente');
    expect(db.tables.establishments[1].address).toBe('Centro de Pasto');
  });

  it('clears the description with null', async () => {
    await service.update(mine.userId, { description: 'Algo' });

    await expect(
      service.update(mine.userId, { description: null }),
    ).resolves.toMatchObject({
      description: null,
    });
  });

  it('keeps profile and establishment together: a failed location change undoes the name', async () => {
    db.client.establishment.update.mockRejectedValueOnce(
      new Error('private-database-details'),
    );

    const result = service.update(mine.userId, {
      businessName: 'Nombre nuevo',
      establishment: { city: 'Ipiales' },
    });

    await expect(result).rejects.toMatchObject({
      response: { statusCode: 500, error: 'Error interno' },
    });
    await expect(result).rejects.not.toThrow('private-database-details');
    expect(
      db.tables.merchantProfiles.find((m) => m.userId === mine.userId)
        ?.businessName,
    ).toBe('Mi panadería');
  });

  it('gives a business left without an establishment one, from the defaults', async () => {
    db.tables.establishments = db.tables.establishments.filter(
      (e) => e.merchantProfileId !== mine.profile.id,
    );

    const updated = await service.update(mine.userId, {
      establishment: { address: 'Calle 10' },
    });

    expect(updated.establishment).toMatchObject({
      name: 'Mi panadería',
      address: 'Calle 10',
      city: 'Pasto',
    });
  });

  it('answers a merchant without a business profile with 404', async () => {
    const stranger = '6c1c3c4e-5b8e-4a55-9b52-2d7d3a8f0a01';

    await expect(service.get(stranger)).rejects.toMatchObject({
      response: {
        statusCode: 404,
        message: 'Completa el perfil de tu negocio para continuar.',
      },
    });
    await expect(
      service.update(stranger, { businessName: 'Otro' }),
    ).rejects.toMatchObject({
      response: { statusCode: 404 },
    });
  });

  it('refuses an empty change', async () => {
    await expect(
      service.update(mine.userId, { establishment: {} }),
    ).rejects.toMatchObject({
      response: { statusCode: 400 },
    });
  });

  it('never reaches another business, whatever the body says', async () => {
    await service.update(theirs.userId, {
      businessName: 'Cambiado por su dueño',
    });

    expect((await service.get(mine.userId)).businessName).toBe('Mi panadería');
  });
});
