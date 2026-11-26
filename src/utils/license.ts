/**
 * ArthFood - Offline HMAC-SHA256 License Verification & Trial Access Utility
 *
 * Matches the key generation format used in `keygen.html` and the native Android
 * counterpart in `app/src/main/java/com/example/licensing/LicenseManager.kt`:
 *   - Single Plan: ₹499 One-Time Lifetime Access
 *   - Key Format:  AF499-XXXX-XXXX-XXXX (HMAC-SHA256 over `${deviceId}:FULL_499`)
 *   - 7-Day Trial: Clock-rollback checks apply ONLY during trial; once activated
 *     and verified for `deviceId`, the app remains permanently unlocked offline.
 */

export const PLAN_PRICE_INR = 499;
export const TRIAL_DAYS = 7;
export const TRIAL_DURATION_MS = TRIAL_DAYS * 24 * 60 * 60 * 1000;
export const CLOCK_ROLLBACK_GRACE_MS = 60 * 60 * 1000; // 1 hour

// Synced with ArthFood Android app (LicenseManager.kt HMAC_PASSPHRASE) & keygen.html.
export const SECRET_PASSPHRASE = "ARTHFOOD_PRO_LIFETIME_2026_HMAC";
export const LICENSE_SECRET_PASSPHRASE = SECRET_PASSPHRASE;

const KEY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const KEY_PREFIX = "AF499";
const KEY_BODY_LENGTH = 12;
const KEY_REGEX = /^AF499-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/;

export interface LicenseRecord {
  deviceId: string;
  isActivated: boolean;
  licenseKey: string;
  activatedAt?: number;
  trialStartedAt: number;
  lastSeenAt: number;
}

export interface ActivationStatusResult {
  deviceId: string;
  isActivated: boolean;
  isLockedOut: boolean;
  isTrialExpired: boolean;
  isClockTampered: boolean;
  daysRemaining: number;
  normalizedKey: string;
  reason: "ACTIVATED" | "TRIAL_ACTIVE" | "TRIAL_EXPIRED" | "CLOCK_TAMPERED" | "INVALID_KEY" | "DEVICE_MISMATCH";
}

/**
 * Strips whitespace/hyphens, uppercases, and returns a normalized raw key string.
 * Example: "af499 abcd efgh ijkl" -> "AF499ABCDEFGHIJKL" (or with AF499 prefix if omitted).
 */
export function normalizeLicenseKey(inputKey: string): string {
  const withoutInvisible = inputKey.replace(/[\u200B-\u200D\uFEFF]/g, "").toUpperCase();
  const tokenMatch = withoutInvisible.match(/AF499[\s-]*([A-Z0-9]{4})[\s-]*([A-Z0-9]{4})[\s-]*([A-Z0-9]{4})/);
  if (tokenMatch) {
    return `${KEY_PREFIX}${tokenMatch[1]}${tokenMatch[2]}${tokenMatch[3]}`;
  }

  const cleaned = withoutInvisible.trim().replace(/[^A-Z0-9]/g, "");
  if (!cleaned) return "";

  if (cleaned.startsWith(KEY_PREFIX)) {
    return cleaned.slice(0, KEY_PREFIX.length + KEY_BODY_LENGTH);
  }

  // Allow users to paste only the 12-char body without "AF499"
  return `${KEY_PREFIX}${cleaned.slice(0, KEY_BODY_LENGTH)}`;
}

/**
 * Normalizes and formats any license key string into `AF499-XXXX-XXXX-XXXX`.
 * Returns an empty string if the input has no alphanumeric characters, or formats
 * cleanly up to the full 12-character key body.
 */
export function formatLicenseKey(inputKey: string): string {
  const normalized = normalizeLicenseKey(inputKey);
  if (!normalized) return "";

  const body = normalized.startsWith(KEY_PREFIX)
    ? normalized.slice(KEY_PREFIX.length)
    : normalized;

  if (!body) return KEY_PREFIX;

  const groups: string[] = [];
  for (let i = 0; i < body.length && i < KEY_BODY_LENGTH; i += 4) {
    groups.push(body.slice(i, i + 4));
  }
  return `${KEY_PREFIX}-${groups.join("-")}`;
}

export function isWellFormedLicenseKey(inputKey: string): boolean {
  return KEY_REGEX.test(formatLicenseKey(inputKey));
}

function constantTimeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

/**
 * Computes the expected `AF499-XXXX-XXXX-XXXX` key for a given `deviceId`
 * using Web Crypto HMAC-SHA256 and the shared secret passphrase.
 */
export async function computeExpectedLicenseKey(
  deviceId: string,
  passphrase: string = SECRET_PASSPHRASE
): Promise<string> {
  const cleanDeviceId = deviceId.trim().toUpperCase();
  if (!cleanDeviceId) {
    throw new Error("Device ID is required to compute a license key.");
  }

  const subtle = globalThis.crypto?.subtle;
  if (!subtle) {
    throw new Error("Web Crypto API (crypto.subtle) is unavailable in this environment.");
  }

  const encoder = new TextEncoder();
  const cryptoKey = await subtle.importKey(
    "raw",
    encoder.encode(passphrase),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await subtle.sign(
    "HMAC",
    cryptoKey,
    encoder.encode(`${cleanDeviceId}:FULL_499`)
  );

  const bytes = new Uint8Array(signature);
  let code = "";
  for (let i = 0; i < KEY_BODY_LENGTH; i++) {
    code += KEY_ALPHABET[bytes[i] & 0x1f];
  }

  return `${KEY_PREFIX}-${code.slice(0, 4)}-${code.slice(4, 8)}-${code.slice(8, 12)}`;
}

/**
 * Verifies a user-supplied license key against `deviceId` using HMAC-SHA256
 * and constant-time comparison. Accepts inputs with/without hyphens or lowercase.
 */
export async function verifyLicenseKey(
  deviceId: string,
  inputKey: string,
  passphrase: string = SECRET_PASSPHRASE
): Promise<boolean> {
  const cleanDeviceId = deviceId.trim().toUpperCase();
  const formattedInput = formatLicenseKey(inputKey);
  if (!cleanDeviceId || !KEY_REGEX.test(formattedInput)) {
    return false;
  }

  const expected = await computeExpectedLicenseKey(cleanDeviceId, passphrase);
  return constantTimeEqual(formattedInput, expected);
}

/**
 * Evaluates whether the app is activated or in an active 7-day trial for `currentDeviceId`.
 * - If `record.isActivated` is true AND `record.licenseKey` verifies for `currentDeviceId`,
 *   returns permanently unlocked (`isActivated: true, isLockedOut: false`) without running
 *   any clock-rollback checks.
 * - Otherwise evaluates the 7-day trial and trial-only clock-rollback tamper check.
 */
export async function checkActivationStatus(
  record: LicenseRecord | null | undefined,
  currentDeviceId: string,
  passphrase: string = SECRET_PASSPHRASE,
  now: number = Date.now()
): Promise<ActivationStatusResult> {
  const cleanDeviceId = currentDeviceId.trim().toUpperCase();

  if (record?.isActivated) {
    const recordDevice = record.deviceId.trim().toUpperCase();
    if (recordDevice && recordDevice !== cleanDeviceId) {
      return {
        deviceId: cleanDeviceId,
        isActivated: false,
        isLockedOut: true,
        isTrialExpired: true,
        isClockTampered: false,
        daysRemaining: 0,
        normalizedKey: "",
        reason: "DEVICE_MISMATCH",
      };
    }

    const formattedKey = formatLicenseKey(record.licenseKey);
    const valid = await verifyLicenseKey(cleanDeviceId, formattedKey, passphrase);
    if (valid) {
      // Permanently unlocked offline forever — never run clock-rollback checks here.
      return {
        deviceId: cleanDeviceId,
        isActivated: true,
        isLockedOut: false,
        isTrialExpired: false,
        isClockTampered: false,
        daysRemaining: TRIAL_DAYS,
        normalizedKey: formattedKey,
        reason: "ACTIVATED",
      };
    }

    return {
      deviceId: cleanDeviceId,
      isActivated: false,
      isLockedOut: true,
      isTrialExpired: true,
      isClockTampered: false,
      daysRemaining: 0,
      normalizedKey: "",
      reason: "INVALID_KEY",
    };
  }

  const trialStart = record?.trialStartedAt && record.trialStartedAt > 0 ? record.trialStartedAt : now;
  const lastSeen = record?.lastSeenAt && record.lastSeenAt > 0 ? record.lastSeenAt : trialStart;

  const isClockTampered = now < lastSeen - CLOCK_ROLLBACK_GRACE_MS || now < trialStart;
  const elapsedMs = Math.max(0, now - trialStart);
  const remainingMs = Math.max(0, TRIAL_DURATION_MS - elapsedMs);
  const daysRemaining = isClockTampered
    ? 0
    : Math.min(TRIAL_DAYS, Math.max(0, Math.ceil(remainingMs / (24 * 60 * 60 * 1000))));
  const isTrialExpired = isClockTampered || elapsedMs >= TRIAL_DURATION_MS;

  return {
    deviceId: cleanDeviceId,
    isActivated: false,
    isLockedOut: isTrialExpired || isClockTampered,
    isTrialExpired,
    isClockTampered,
    daysRemaining,
    normalizedKey: "",
    reason: isClockTampered
      ? "CLOCK_TAMPERED"
      : isTrialExpired
        ? "TRIAL_EXPIRED"
        : "TRIAL_ACTIVE",
  };
}
