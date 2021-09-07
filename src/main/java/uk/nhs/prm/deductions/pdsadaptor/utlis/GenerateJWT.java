package uk.nhs.prm.deductions.pdsadaptor.utlis;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
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

    @Value("jwtPrivateKey")
    private String jwtPrivateKey;

    @Value("jwtApiKey")
    private String jwtApiKey;

    @Value("accessTokenEndpoint")
    private String accessTokenEndpoint;

    @Value("jwtKeyId")
    private String jwtKeyId;

    public String createSignedJWT() throws Exception {

        PrivateKey rsaJWK = getPrivateKey(jwtPrivateKey);

        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(rsaJWK);

        // Prepare JWT with claims set
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(jwtApiKey)
                .issuer(jwtApiKey)
                .jwtID(UUID.randomUUID().toString())
                .audience(accessTokenEndpoint)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(jwtKeyId).type(JOSEObjectType.JWT).build(),
                claimsSet);
        // Compute the RSA signature
        signedJWT.sign(signer);

        // To serialize to compact form
        String signedJWToken = signedJWT.serialize();
        return signedJWToken;
    }

    public PrivateKey getPrivateKey(String privateKeyString) throws IOException {
        PEMParser pemParser = new PEMParser(new StringReader(privateKeyString));
        Security.addProvider(new BouncyCastleProvider());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        Object object = pemParser.readObject();
        KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
        return kp.getPrivate();
    }
}