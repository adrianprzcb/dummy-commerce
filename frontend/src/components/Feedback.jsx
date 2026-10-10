export default function Feedback({ loading, error, message }) {
  return <>
    {loading && <p role="status">Cargando…</p>}
    {error && <p className="notice error" role="alert">{error.message || error}</p>}
    {message && <p className="notice success" role="status">{message}</p>}
  </>;
}
