import {
  PICKUP_CODE_ALPHABET,
  PICKUP_CODE_LENGTH,
  pickupCodeBody,
  randomPickupCode,
} from './pickup-code';

describe('randomPickupCode', () => {
  it('looks like ZW-XXXX', () => {
    for (let i = 0; i < 200; i++) {
      expect(randomPickupCode()).toMatch(/^ZW-[2-9A-HJ-NP-Z]{4}$/);
    }
  });

  it('never uses characters that read alike', () => {
    expect(PICKUP_CODE_ALPHABET).not.toMatch(/[01OI]/);
    expect(new Set(PICKUP_CODE_ALPHABET).size).toBe(32);
    const bodies = Array.from({ length: 500 }, () =>
      randomPickupCode().slice(3),
    ).join('');
    expect(bodies).not.toMatch(/[01OI]/);
    expect(bodies).toHaveLength(500 * PICKUP_CODE_LENGTH);
  });

  it('varies from one call to the next', () => {
    const codes = new Set(Array.from({ length: 100 }, randomPickupCode));
    expect(codes.size).toBeGreaterThan(90);
  });
});

describe('pickupCodeBody', () => {
  it.each([
    ['ZW-7KQ3', '7KQ3'],
    ['zw-7kq3', '7KQ3'],
    [' ZW 7KQ3 ', '7KQ3'],
    ['ZW7KQ3', '7KQ3'],
    ['7kq3', '7KQ3'],
    // A body that itself starts with ZW keeps it when typed without the prefix
    ['ZWAB', 'ZWAB'],
    ['ZW-ZWAB', 'ZWAB'],
  ])('reads %p as %p', (typed, body) => {
    expect(pickupCodeBody(typed)).toBe(body);
  });

  it('matches what randomPickupCode produced', () => {
    const code = randomPickupCode();
    expect(pickupCodeBody(code.toLowerCase())).toBe(code.slice(3));
  });
});
