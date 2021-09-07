package uk.nhs.prm.deductions.pdsadaptor.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Date;
import java.util.UUID;

@Component
public class GenerateJWT {
    private final String jwtPrivateKey;
    private final String jwtApiKey;
    private final String accessTokenEndpoint;
    private final String jwtKeyId;

    public GenerateJWT(@Value("jwtPrivateKey") String jwtPrivateKey, @Value("jwtApiKey") String jwtApiKey, @Value("accessTokenEndpoint") String accessTokenEndpoint, @Value("jwtKeyId") String jwtKeyId) {
        this.jwtPrivateKey = jwtPrivateKey;
        this.jwtApiKey = jwtApiKey;
        this.accessTokenEndpoint = accessTokenEndpoint;
        this.jwtKeyId = jwtKeyId;
    }

    public String createSignedJWT() throws IOException, JOSEException {
        PrivateKey rsaJWK = getPrivateKey(jwtPrivateKey);
        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(rsaJWK);
        // Prepare JWT with claims set
        JWTClaimsSet claimsSet = getClaimsSet();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(jwtKeyId).type(JOSEObjectType.JWT).build(), claimsSet);
        // Compute the RSA signature
        signedJWT.sign(signer);

        // To serialize to compact form
        return signedJWT.serialize();
    }

    private JWTClaimsSet getClaimsSet() {
        return new JWTClaimsSet.Builder()
                .subject(jwtApiKey)
                .issuer(jwtApiKey)
                .jwtID(UUID.randomUUID().toString())
                .audience(accessTokenEndpoint)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
    }

    private PrivateKey getPrivateKey(String privateKeyString) throws IOException {
        PEMParser pemParser = new PEMParser(new StringReader(privateKeyString));
        Security.addProvider(new BouncyCastleProvider());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        Object object = pemParser.readObject();
        KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
        return kp.getPrivate();
    }
}