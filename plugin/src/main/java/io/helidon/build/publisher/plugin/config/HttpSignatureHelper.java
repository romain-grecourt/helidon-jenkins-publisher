package io.helidon.build.publisher.plugin.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * HTTP signature utility.
 */
public class HttpSignatureHelper {

    /**
     * Invoke {@code /ping} on the given URL to test authentication with the given private key.
     * @param url URL to use
     * @param key SHA256 RSA private key
     * @return response code
     * @throws IOException if an IO error occurs
     * @throws URISyntaxException if an error occurs when converting the URL to a URI
     */
    public static int test(URL url, String key) throws IOException, URISyntaxException {
        String rawUrl = url.toExternalForm();
        if (rawUrl.endsWith("/")) {
            rawUrl = rawUrl.substring(0, rawUrl.length() - 2);
        }
        URL testUrl = URI.create(rawUrl + "/ping").toURL();
        URLConnection con = testUrl.openConnection();
        if (!(con instanceof HttpURLConnection)) {
            throw new IllegalStateException("Not an HttpURLConnection");
        }
        HttpURLConnection hcon = (HttpURLConnection) con;
        hcon.addRequestProperty("Signature", signatureHeader(sign("Host: " + url.getAuthority()+ "\n", key)));
        hcon.setRequestMethod("GET");
        hcon.setConnectTimeout(20 * 1000);
        hcon.setReadTimeout(20 * 1000);
        return hcon.getResponseCode();
    }

    /**
     * Create the {@code Signature} header value.
     * @param signature signature payload
     * @return String
     */
    public static String signatureHeader(String signature) {
        if (signature != null) {
            return "keyId=\"backend-key\",algorithm=\"rsa-sha256\",headers=\"Host\","
                    + "signature=\"" + signature + "\"";
        } else {
            return null;
        }
    }

    /**
     * Sign a payload.
     * @param payload payload to sign
     * @param key SHA256 RSA private key
     * @return Base64 encoded signed payload
     */
    public static String sign(String payload, String key) {
        if (key == null) {
            return null;
        }
        return new String(Base64.getEncoder().encode(signRsaSha256(payload, key)), StandardCharsets.UTF_8);
    }

    private static byte[] signRsaSha256(String payload, String pkey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            PrivateKey key = KeyFactory
                    .getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pkey)));
            signature.initSign(key);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException(e);
        }
    }
}
