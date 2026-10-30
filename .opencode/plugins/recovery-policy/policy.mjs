/**
 * Dependency-free retry classification for the OpenCode V2 recovery plugin.
 * Does not perform provider failover. It only makes bounded retry decisions.
 */
const HARD_STOP_PATTERNS = [
  /invalid api key/i,
  /api key.{0,24}(expired|revoked|invalid)/i,
  /unauthori[sz]ed/i,
  /authentication failed/i,
  /invalid credentials/i,
  /credential.{0,20}expired/i,
  /token.{0,20}expired/i,
  /permission denied/i,
  /billing/i,
  /insufficient credits?/i,
  /out of credits/i,
  /payment required/i,
  /daily quota/i,
  /quota.{0,24}(exhausted|exceeded|daily|per day)/i,
  /(?:daily|per day|per-day).{0,32}(limit|quota|token|request)/i,
  /tokens per day/i,
  /requests per day/i,
  /free tier.{0,32}(limit|quota|exceeded)/i,
  /resource_exhausted/i,
];

const AUTH_STATUS = new Set([401, 403]);
const NON_RETRYABLE_STATUS = new Set([400, 404, 405, 413, 415, 422]);
const TRANSIENT_STATUS = new Set([408, 425, 429, 500, 502, 503, 504]);
const TRANSIENT_WORDS = /timeout|timed out|network|connection reset|connection refused|temporarily unavailable|temporary failure|overloaded|socket hang up|fetch failed|failed to fetch|econnreset|econnrefused|ehostunreach|enotfound|eai_again|dns lookup|networkerror|rate limit|too many requests|service unavailable|server error/i;

/**
 * @param {{error:{type?:string,message?:string,status?:number},attempt:number,decision?:{retry:boolean,delay?:number}}} event
 * @returns {{decision:{retry:boolean,delay?:number}, reason:string}|null}
 * `null` means preserve OpenCode's own decision for an unclassified error.
 */
export function decideRetry(event) {
  const error = event?.error ?? {};
  const status = Number.isFinite(error.status) ? error.status : undefined;
  const type = String(error.type ?? '');
  const message = String(error.message ?? '');
  const fingerprint = `${type} ${message}`;
  const attempt = Number.isFinite(event?.attempt) ? event.attempt : 1;

  if (AUTH_STATUS.has(status) || HARD_STOP_PATTERNS.some((pattern) => pattern.test(fingerprint))) {
    return { decision: { retry: false }, reason: 'auth_or_hard_quota_stop' };
  }
  if (NON_RETRYABLE_STATUS.has(status) || /provider\.invalid-request/i.test(type)) {
    return { decision: { retry: false }, reason: 'non_retryable_request' };
  }
  if (attempt >= 3) {
    return { decision: { retry: false }, reason: 'retry_attempt_limit_reached' };
  }

  const transient = (status !== undefined && TRANSIENT_STATUS.has(status)) || TRANSIENT_WORDS.test(fingerprint) || /provider\.(timeout|network|unavailable|overloaded|rate_limit|server_error)/i.test(type);
  if (!transient) return null;

  let delay;
  if (status === 429) delay = attempt === 1 ? 10_000 : 20_000;
  else if (status !== undefined && status >= 500) delay = attempt === 1 ? 2_000 : 5_000;
  else delay = attempt === 1 ? 1_500 : 4_000;

  return { decision: { retry: true, delay }, reason: status === 429 ? 'temporary_rate_limit_retry' : 'transient_transport_retry' };
}
