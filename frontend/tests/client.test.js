import test from 'node:test';
import assert from 'node:assert/strict';
import { ApiError, createApiClient } from '../src/api/client.js';

const tokens = { accessToken: 'new-access', refreshToken: 'new-refresh', accessTokenExpiresIn: 900 };
function response(status, body = null) {
  return { status, ok: status >= 200 && status < 300, text: async () => body === null ? '' : JSON.stringify(body) };
}
function setup(fetchImpl) {
  let value = { accessToken: 'old-access', refreshToken: 'old-refresh', user: { id: 'user-1' }, expiresAt: 999999 };
  const session = { get: () => value, save: next => { value = next; } };
  const client = createApiClient({ urls: { users: 'http://users', orders: 'http://orders' }, session, fetchImpl, now: () => 1000 });
  return { client, session };
}

test('checkout sends exact items and authenticated header', async () => {
  const { client } = setup(async (url, options) => {
    assert.equal(url, 'http://orders/api/orders');
    assert.equal(options.headers.Authorization, 'Bearer old-access');
    assert.deepEqual(JSON.parse(options.body), { items: [{ productId: 'product', quantity: 2, unitPrice: 12.34 }] });
    return response(201, { id: 'order' });
  });
  assert.deepEqual(await client.request('orders', '/api/orders', { method: 'POST', body: { items: [{ productId: 'product', quantity: 2, unitPrice: 12.34 }] } }), { id: 'order' });
});

test('two concurrent 401 responses share one refresh and use rotated token', async () => {
  let refreshes = 0;
  const { client, session } = setup(async (url, options) => {
    if (url.endsWith('/refresh')) {
      refreshes++;
      assert.deepEqual(JSON.parse(options.body), { refreshToken: 'old-refresh' });
      await new Promise(resolve => setTimeout(resolve, 5));
      return response(200, tokens);
    }
    return options.headers.Authorization === 'Bearer old-access' ? response(401) : response(200, { ok: true });
  });
  const results = await Promise.all([client.request('orders', '/one'), client.request('orders', '/two')]);
  assert.deepEqual(results, [{ ok: true }, { ok: true }]);
  assert.equal(refreshes, 1);
  assert.equal(session.get().refreshToken, 'new-refresh');
});

test('a second 401 clears the session without an infinite refresh loop', async () => {
  let requests = 0, refreshes = 0;
  const { client, session } = setup(async url => {
    if (url.endsWith('/refresh')) { refreshes++; return response(200, tokens); }
    requests++; return response(401);
  });
  await assert.rejects(client.request('orders', '/orders'), error => error.status === 401);
  assert.equal(requests, 2); assert.equal(refreshes, 1); assert.equal(session.get(), null);
});

test('revoked refresh token clears authentication', async () => {
  const { client, session } = setup(async () => response(401));
  await assert.rejects(client.refresh(), error => error.status === 401);
  assert.equal(session.get(), null);
});

test('403 does not attempt refresh or discard a valid session', async () => {
  let calls = 0;
  const { client, session } = setup(async () => { calls++; return response(403); });
  await assert.rejects(client.request('orders', '/admin'), error => error.status === 403);
  assert.equal(calls, 1); assert.ok(session.get());
});

test('logout during refresh cannot resurrect the cleared session', async () => {
  let resolve;
  const { client, session } = setup(() => new Promise(done => { resolve = done; }));
  const pending = client.refresh();
  session.save(null);
  resolve(response(200, tokens));
  await assert.rejects(pending, error => error.status === 401);
  assert.equal(session.get(), null);
});

test('an expired access token is refreshed before making the request', async () => {
  let refreshes = 0;
  const { client, session } = setup(async (url, options) => {
    if (url.endsWith('/refresh')) { refreshes++; return response(200, tokens); }
    assert.equal(options.headers.Authorization, 'Bearer new-access');
    return response(200, []);
  });
  session.save({ ...session.get(), expiresAt: 1001 });
  await client.request('orders', '/orders');
  assert.equal(refreshes, 1);
});

test('a late 401 from another account does not replay its request as the new user', async () => {
  let resolve, calls = 0;
  const { client, session } = setup(() => { calls++; return new Promise(done => { resolve = done; }); });
  const request = client.request('orders', '/orders', { method: 'POST', body: { items: [] } });
  session.save({ ...session.get(), accessToken: 'different', user: { id: 'user-2' } });
  resolve(response(401));
  await assert.rejects(request, error => error.status === 401);
  assert.equal(calls, 1); assert.equal(session.get().user.id, 'user-2');
});

test('technical errors are sanitized and price conflicts remain useful', () => {
  assert.doesNotMatch(new ApiError(500, { detail: 'postgres password=secret stacktrace' }).message, /postgres|secret|stacktrace/);
  assert.match(new ApiError(409, { detail: 'Product price changed; refresh catalogue' }).message, /precio ha cambiado/);
});
