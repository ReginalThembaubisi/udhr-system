// Translations for the login screen and the patient portal. See
// locales/en.js for how to add a language.
import en from './locales/en.js';

const LOCALES = { en };
const STORAGE_KEY = 'udhr_language';

export const LANGUAGES = Object.entries(LOCALES).map(([code, strings]) => ({ code, name: strings['language.name'] || code }));

const readSaved = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved && LOCALES[saved] ? saved : 'en';
  } catch {
    return 'en';
  }
};

let current = readSaved();

export function getLanguage() {
  return current;
}

export function setLanguage(code) {
  if (!LOCALES[code]) return;
  current = code;
  try { localStorage.setItem(STORAGE_KEY, code); } catch { /* private mode */ }
  document.documentElement.lang = code;
}

/** t('portal.hi', { name: 'Thandi' }) gives "Hi, Thandi" in the current language. */
export function t(key, vars) {
  const text = LOCALES[current]?.[key] ?? en[key] ?? key;
  return vars ? text.replace(/\{(\w+)\}/g, (match, name) => (name in vars ? vars[name] : match)) : text;
}
