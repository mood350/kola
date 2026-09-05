// Shared helpers for Mock*Repository implementations only.

/** Simulated network latency so loading states are real, not instant. */
export const delay = (ms = 250) => new Promise((resolve) => setTimeout(resolve, ms));

/** Deep-clone so callers can't mutate the in-memory "database" by reference. */
export const clone = (value) => JSON.parse(JSON.stringify(value));
