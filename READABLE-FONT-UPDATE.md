# UDHR readable typography update (4 November 2026)

- Increased undersized CSS text across all staff and patient screens. Common 10–14px typography is now approximately 13–16.5px; regular text is typically 16–18px, with headings larger.
- Raised the root `rem` reference from 16px to 17px to improve rem-based UI text.
- Increased inline React presentation-only `fontSize` values to the same scale, including small status labels and table details.
- Preserved registration layouts, input handlers, data, validation, APIs, authentication, and backend implementation. No feature logic changed.
- Large headings and dimension values were not globally zoomed; this prevents unnecessary overflow and keeps the layout responsive.

Run the usual frontend (`npm.cmd install` then `npm.cmd run dev`) and backend separately. Browser refresh may be needed to see the new CSS.
