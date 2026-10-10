export default function Feedback({ loading, error, message }) {
  return <>
    {loading && <p className="loading-state" role="status"><span aria-hidden="true" />Cargando…</p>}
    {error && <p className="notice error" role="alert">{error.message || error}</p>}
    {message && <p className="notice success" role="status">{message}</p>}
  </>;
}
