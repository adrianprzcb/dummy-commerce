export class ApiError extends Error {
  constructor(status, problem) {
    const defaults = {
      400: 'Revisa los campos del formulario.',
      401: 'Tu sesión ha caducado. Inicia sesión de nuevo.',
      403: 'No tienes permiso para realizar esta operación.',
      404: 'No se ha encontrado el recurso solicitado.',
      409: 'La operación entra en conflicto con el estado actual.',
    };
    let message = defaults[status] || 'No se pudo completar la operación. Inténtalo de nuevo.';
    if (status === 409 && /price changed/i.test(problem?.detail)) {
      message = 'El precio ha cambiado. Actualiza el carrito antes de comprar.';
    } else if (status === 409 && /product is unavailable/i.test(problem?.detail)) {
      message = 'Un producto ya no está disponible. Actualiza el carrito.';
    } else if (status < 500 && status !== 401 && status !== 403 && problem?.detail) {
      message = String(problem.detail).slice(0, 250);
    }
    super(message);
    this.status = status;
  }
}

export function createApiClient({ urls, session, fetchImpl = globalThis.fetch, now = Date.now }) {
  let refreshing = null;

  async function send(service, path, { method = 'GET', body, signal } = {}, token) {
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (token) headers.Authorization = `Bearer ${token}`;
    let response;
    try {
      response = await fetchImpl(`${urls[service]}${path}`, {
        method, headers, signal,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch (error) {
      if (error.name === 'AbortError') throw error;
      throw new Error('No se puede conectar con el servicio. Comprueba que está disponible.', { cause: error });
    }
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { /* Algunos errores de red no incluyen JSON. */ }
    if (!response.ok) throw new ApiError(response.status, data);
    return data;
  }

  function refresh() {
    if (refreshing) return refreshing;
    const previous = session.get();
    if (!previous?.refreshToken) return Promise.reject(new ApiError(401));
    refreshing = (async () => {
      try {
        const tokens = await send('users', '/api/auth/refresh', {
          method: 'POST', body: { refreshToken: previous.refreshToken },
        });
        if (session.get()?.refreshToken !== previous.refreshToken) throw new ApiError(401);
        session.save({ ...previous, ...tokens, expiresAt: now() + tokens.accessTokenExpiresIn * 1000 });
        return tokens.accessToken;
      } catch (error) {
        if (error.status === 401 && session.get()?.refreshToken === previous.refreshToken) session.save(null);
        throw error;
      } finally { refreshing = null; }
    })();
    return refreshing;
  }

  async function request(service, path, options = {}) {
    if (options.auth === false) return send(service, path, options);
    let value = session.get();
    if (!value?.accessToken) throw new ApiError(401);
    let refreshed = false;
    if (value.expiresAt <= now() + 15000) {
      await refresh();
      refreshed = true;
      value = session.get();
    }
    try { return await send(service, path, options, value.accessToken); }
    catch (error) {
      if (error.status !== 401 || options.signal?.aborted) throw error;
      if (refreshed) {
        if (session.get()?.accessToken === value.accessToken) session.save(null);
        throw error;
      }
      if (!session.get()) throw error;
      if (value.user?.id !== session.get().user?.id) throw error;
      if (session.get().accessToken === value.accessToken) await refresh();
      const retryToken = session.get()?.accessToken;
      if (!retryToken) throw error;
      try { return await send(service, path, options, retryToken); }
      catch (retryError) {
        if (retryError.status === 401 && session.get()?.accessToken === retryToken) session.save(null);
        throw retryError;
      }
    }
  }

  return { request, refresh, waitForRefresh: () => refreshing || Promise.resolve() };
}
