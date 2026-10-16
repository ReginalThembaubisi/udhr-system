// Every API call goes through apiFetch, so session handling lives in one place.
//
// The access token is kept here, in memory only (fix list P1-3): never in
// localStorage, where any script on the page could read it. The refresh token
// is an HttpOnly cookie the browser sends to /api/auth/* by itself; this code
// never sees it.
//
// Access tokens last 30 minutes. When a call comes back 401, apiFetch renews
// the token once through /api/auth/refresh and repeats the call; if renewal
// fails, the session has really ended and onSessionExpired runs (fix list P1-2).

let accessToken = '';
let refreshInFlight = null;
let sessionExpiredHandler = () => {};

export function setAccessToken(token) {
  accessToken = token || '';
}

export function getAccessToken() {
  return accessToken;
}

export function onSessionExpired(handler) {
  sessionExpiredHandler = handler;
}

// Trades the refresh cookie for a new access token. Calls that fail at the same
// moment share one refresh instead of each spending the single-use cookie.
// Returns the session (token, role, name...) or null if it has ended.
export function refreshSession() {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      // Two tabs can refresh at the same instant; the server asks the slower one
      // to retry, by which time the browser holds the new cookie.
      for (let attempt = 0; attempt < 2; attempt++) {
        try {
          const response = await fetch('/api/auth/refresh', { method: 'POST' });
          if (response.ok) {
            const session = await response.json();
            accessToken = session.token;
            return session;
          }
        } catch {
          // network trouble: treated like a failed refresh
        }
        if (attempt === 0) await new Promise(resolve => setTimeout(resolve, 600));
      }
      return null;
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

const withToken = (options) => ({
  ...options,
  headers: {
    ...(options.headers || {}),
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
  },
});

export async function apiFetch(url, options = {}) {
  const response = await fetch(url, withToken(options));
  // Sign-in, refresh and sign-out answer 401 for their own reasons.
  if (response.status !== 401 || url.startsWith('/api/auth/') || !accessToken) {
    return response;
  }
  const session = await refreshSession();
  if (!session) {
    sessionExpiredHandler();
    return response;
  }
  return fetch(url, withToken(options));
}
