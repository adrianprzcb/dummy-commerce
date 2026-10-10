import { useEffect, useState } from 'react';

export function useLoad(loader) {
  const [version, setVersion] = useState(0);
  const [result, setResult] = useState({ data: null, loading: true, error: null });
  useEffect(() => {
    const controller = new AbortController();
    setResult(previous => ({ ...previous, loading: true, error: null }));
    loader(controller.signal)
      .then(data => { if (!controller.signal.aborted) setResult({ data, loading: false, error: null }); })
      .catch(error => { if (!controller.signal.aborted) setResult({ data: null, loading: false, error }); });
    return () => controller.abort();
  }, [loader, version]);
  return { ...result, reload: () => setVersion(value => value + 1) };
}
