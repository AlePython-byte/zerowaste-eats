import { AuthController } from './auth.controller';

describe('AuthController', () => {
  const controller = new AuthController();

  it.each(['customer@example.test', null])(
    'returns the authenticated identity with email %s',
    (email) => {
      const user = {
        id: 'b7a2c1b3-736e-45fc-8df5-c1dd6cc88d13',
        email,
        extra: 'not-returned',
      };
      expect(controller.getCurrentUser(user)).toEqual({ id: user.id, email });
    },
  );
});
