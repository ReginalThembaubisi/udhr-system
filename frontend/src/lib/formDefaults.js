// Screen-wide defaults that every form and button in the app gets, without
// repeating them on ~100 individual elements in App.jsx.
//
// 1. Length limits: a text box without its own maxLength stops at 255
//    characters (a long-text box at 2000), matching the database's text
//    columns, so typing can't run past what the server will store.
// 2. Tooltips: a button, text box or list without its own title gets one from
//    its accessible label, its placeholder or its visible text, so hovering
//    over any control says what it does.
// 3. F1 opens the quick help page.

const TEXT_TYPES = new Set(['text', 'search', 'email', 'tel', 'url', 'password', '']);
const TEXT_LIMIT = 255;
const LONG_TEXT_LIMIT = 2000;

function tooltipFor(el) {
  const label = el.getAttribute('aria-label') || el.getAttribute('placeholder');
  if (label) return label.trim();
  if (el.tagName === 'BUTTON') {
    const text = (el.textContent || '').replace(/\s+/g, ' ').trim();
    if (text && text.length <= 60) return text;
  }
  if (el.id) {
    const labelEl = document.querySelector(`label[for="${CSS.escape(el.id)}"]`);
    const text = labelEl && labelEl.textContent.replace(/\s+/g, ' ').trim();
    if (text) return text;
  }
  return '';
}

function apply(el) {
  if (el.tagName === 'INPUT' && TEXT_TYPES.has((el.getAttribute('type') || '').toLowerCase())
      && !el.hasAttribute('maxlength')) {
    el.setAttribute('maxlength', String(TEXT_LIMIT));
  }
  if (el.tagName === 'TEXTAREA' && !el.hasAttribute('maxlength')) {
    el.setAttribute('maxlength', String(LONG_TEXT_LIMIT));
  }
  if (!el.hasAttribute('title')) {
    const tip = tooltipFor(el);
    if (tip) el.setAttribute('title', tip);
  }
}

const SELECTOR = 'button, input, textarea, select';

function applyWithin(root) {
  if (root.nodeType !== 1) return;
  if (root.matches(SELECTOR)) apply(root);
  root.querySelectorAll(SELECTOR).forEach(apply);
}

export function installFormDefaults() {
  applyWithin(document.body);
  new MutationObserver((mutations) => {
    for (const m of mutations) m.addedNodes.forEach(applyWithin);
  }).observe(document.body, { childList: true, subtree: true });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'F1') {
      e.preventDefault();
      window.open('/help.html', '_blank', 'noopener');
    }
  });
}
