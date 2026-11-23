export async function api(path, { method = "GET", body, token, signal } = {}) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = "Bearer " + token;
  const response = await fetch("/api" + path, {
    method,
    headers,
    signal,
    cache: "no-store",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    const detail = await response.json().catch(() => null);
    const error = new Error(detail?.message || "La demande a échoué (HTTP " + response.status + ").");
    error.status = response.status;
    throw error;
  }
  return response.json();
}
