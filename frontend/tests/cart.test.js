import test from 'node:test';
import assert from 'node:assert/strict';
import { cartTotal, validQuantity } from '../src/format.js';

test('cart totals use cents for decimal prices and quantities', () => {
  assert.equal(cartTotal([{ unitPrice: 0.1, quantity: 1 }, { unitPrice: 0.2, quantity: 1 }]), 0.3);
  assert.equal(cartTotal([{ unitPrice: 12.34, quantity: 3 }]), 37.02);
  assert.equal(cartTotal([]), 0);
});
test('invalid quantities cannot enter the cart', () => {
  for (const value of [0, -1, 1.5, 1000, NaN, Infinity]) assert.equal(validQuantity(value), false);
  assert.equal(validQuantity(1), true); assert.equal(validQuantity(999), true);
});
