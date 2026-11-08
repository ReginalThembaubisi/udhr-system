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
let accessReasonHandler = null;
let reasonInFlight = null;

export function setAccessToken(token) {
  accessToken = token || '';
}

export function getAccessToken() {
  return accessToken;
}

export function onSessionExpired(handler) {
  sessionExpiredHandler = handler;
}

// Opening a patient who isn't on today's list at your clinic needs a reason
// (the server answers 428 with reasonRequired). The handler shows the dialog
// and resolves with the reason, or null if the person cancels.
export function onAccessReasonRequired(handler) {
  accessReasonHandler = handler;
}

async function withAccessReason(url, options, response) {
  if (response.status !== 428 || !accessReasonHandler) {
    return response;
  }
  let body;
  try {
    body = await response.clone().json();
  } catch {
    return response;
  }
  if (!body?.reasonRequired) {
    return response;
  }
  // Opening one patient fires several reads at once: ask once, share the answer.
  if (!reasonInFlight) {
    reasonInFlight = Promise.resolve(accessReasonHandler(body.message))
      .finally(() => setTimeout(() => { reasonInFlight = null; }, 0));
  }
  const reason = await reasonInFlight;
  if (!reason) {
    return response;
  }
  const headers = { ...(options.headers || {}), 'X-Access-Reason': encodeURIComponent(reason) };
  return fetch(url, withToken({ ...options, headers }));
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

// One banner for "the database can't be reached" (rubric: what happens if
// the database is moved), on every screen, instead of each screen failing
// in its own way. Cleared by the next request that succeeds.
let databaseHandler = () => {};
export function onDatabaseStatus(handler) {
  databaseHandler = handler;
}

function noteDatabase(response) {
  if (response.status === 503) {
    response.clone().json()
      .then(body => { if (body?.databaseUnavailable) databaseHandler(false, body.message); })
      .catch(() => {});
  } else if (response.ok) {
    databaseHandler(true);
  }
  return response;
}

export async function apiFetch(url, options = {}) {
  let response = noteDatabase(await fetch(url, withToken(options)));
  // Sign-in, refresh and sign-out answer 401 for their own reasons.
  if (response.status === 401 && !url.startsWith('/api/auth/') && accessToken) {
    const session = await refreshSession();
    if (!session) {
      sessionExpiredHandler();
      return response;
    }
    response = noteDatabase(await fetch(url, withToken(options)));
  }
  return withAccessReason(url, options, response);
}
