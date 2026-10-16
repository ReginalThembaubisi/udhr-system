package com.udhr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Six-digit codes from an authenticator app (Google Authenticator, Microsoft
 * Authenticator and the like), the standard TOTP scheme of RFC 6238: an
 * HMAC-SHA1 of the current 30-second window, keyed by a secret shared once
 * through a QR code.
 *
 * With it, a stolen staff password is no longer enough to sign in. Secrets are
 * stored encrypted (AES-GCM, key derived from JWT_SECRET), so a copy of the
 * database alone can't generate codes. Changing JWT_SECRET therefore means
 * staff set up their app again (a system administrator can reset it).
 */
@Service
public class TotpService {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PERIOD_SECONDS = 30;
    // One window either side covers a phone clock that is up to 30 seconds out.
    private static final int WINDOW = 1;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** What the QR code holds; authenticator apps read this format. */
    public String otpauthUri(String accountName, String secret) {
        String label = URLEncoder.encode("UDHR:" + accountName, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=UDHR&digits=6&period=" + PERIOD_SECONDS;
    }

    /**
     * The time window the code belongs to, or -1 if it is wrong. A window at
     * or before lastUsedStep is refused, so a code can't be used twice (someone
     * reading it over a shoulder can't replay it).
     */
    public long verify(String secret, String code, Long lastUsedStep) {
        if (secret == null || code == null || !code.trim().matches("\\d{6}")) {
            return -1;
        }
        long now = System.currentTimeMillis() / 1000 / PERIOD_SECONDS;
        for (long step = now - WINDOW; step <= now + WINDOW; step++) {
            if (lastUsedStep != null && step <= lastUsedStep) {
                continue;
            }
            if (MessageDigest.isEqual(codeAt(secret, step).getBytes(StandardCharsets.UTF_8), code.trim().getBytes(StandardCharsets.UTF_8))) {
                return step;
            }
        }
        return -1;
    }

    /** The 6-digit code for a 30-second window (public for the RFC test vectors). */
    public static String codeAt(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(base32Decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException("Could not compute the sign-in code", e);
        }
    }

    // ---- storing secrets ------------------------------------------------------------

    public String encrypt(String secret) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception e) {
            throw new IllegalStateException("Could not store the sign-in secret", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, all, 0, 12));
            return new String(cipher.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Usually JWT_SECRET changed since the app was set up.
            return null;
        }
    }

    private SecretKeySpec key() throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(("udhr-totp:" + jwtSecret).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hash, "AES");
    }

    // ---- base32 (what authenticator apps use for secrets) ----------------------------

    static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }
        return out.toString();
    }

    static byte[] base32Decode(String text) {
        String clean = text.replace(" ", "").replace("=", "").toUpperCase();
        ByteBuffer out = ByteBuffer.allocate(clean.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : clean.toCharArray()) {
            int value = BASE32.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Not a base32 secret");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) ((buffer >> (bits - 8)) & 0xff));
                bits -= 8;
            }
        }
        return out.array();
    }
}
