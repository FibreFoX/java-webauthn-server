/*
 * Copyright 2014 Yubico.
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file or at
 * https://developers.google.com/open-source/licenses/bsd
 */

package com.yubico.webauthn;

import com.yubico.webauthn.data.ByteArray;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

class BouncyCastleCrypto implements Crypto {

    private static final Provider provider = new BouncyCastleProvider();

    public Provider getProvider() {
        return provider;
    }

    @Override
    public boolean verifySignature(X509Certificate attestationCertificate, ByteArray signedBytes, ByteArray signature) {
        return verifySignature(attestationCertificate.getPublicKey(), signedBytes, signature);
    }

    @Override
    public boolean verifySignature(PublicKey publicKey, ByteArray signedBytes, ByteArray signature) {
        try {
            Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA", provider);
            ecdsaSignature.initVerify(publicKey);
            ecdsaSignature.update(signedBytes.getBytes());
            return ecdsaSignature.verify(signature.getBytes());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(
                String.format(
                    "Failed to verify signature. This could be a problem with your JVM environment, or a bug in webauthn-server-core. Public key: %s, signed data: %s , signature: %s",
                    publicKey,
                    signedBytes.getBase64Url(),
                    signature.getBase64Url()
                ),
                e
            );
        }
    }

    @Override
    public PublicKey decodePublicKey(ByteArray encodedPublicKey) {
        try {
            X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
            ECPoint point;
            try {
                point = curve.getCurve().decodePoint(encodedPublicKey.getBytes());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                    "Could not parse user public key: " + encodedPublicKey.getBase64Url(),
                    e
                );
            }

            return KeyFactory.getInstance("ECDSA", provider).generatePublic(
                    new ECPublicKeySpec(point,
                            new ECParameterSpec(
                                    curve.getCurve(),
                                    curve.getG(),
                                    curve.getN(),
                                    curve.getH()
                            )
                    )
            );
        } catch (GeneralSecurityException e) { //This should not happen
            throw new RuntimeException(
                "Failed to decode public key: " + encodedPublicKey.getBase64Url(),
                e
            );
        }
    }

    @Override
    public ByteArray hash(ByteArray bytes) {
        try {
            return new ByteArray(MessageDigest.getInstance("SHA-256", provider).digest(bytes.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteArray hash(String str) {
        return hash(new ByteArray(str.getBytes()));
    }
}
