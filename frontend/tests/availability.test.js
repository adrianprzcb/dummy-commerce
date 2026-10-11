import test from 'node:test';
import assert from 'node:assert/strict';
import { productAvailability } from '../src/productAvailability.js';

test('active products show actual stock, including inventory not yet created', () => {
  assert.equal(productAvailability({ status: 'ACTIVE', availableQuantity: 0 }), 'OUT_OF_STOCK');
  assert.equal(productAvailability({ status: 'ACTIVE', availableQuantity: 3 }), 'IN_STOCK');
  assert.equal(productAvailability({ status: 'ACTIVE', availableQuantity: null }), 'STOCK_UNKNOWN');
  assert.equal(productAvailability({ status: 'ACTIVE' }), 'STOCK_UNKNOWN');
});
test('administrative product status takes precedence over stock', () => {
  assert.equal(productAvailability({ status: 'INACTIVE', availableQuantity: 3 }), 'INACTIVE');
  assert.equal(productAvailability({ status: 'DISCONTINUED', availableQuantity: 0 }), 'DISCONTINUED');
});
