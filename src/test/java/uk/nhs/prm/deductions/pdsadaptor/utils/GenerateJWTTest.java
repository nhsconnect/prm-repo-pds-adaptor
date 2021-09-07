package uk.nhs.prm.deductions.pdsadaptor.utils;

import org.junit.jupiter.api.Test;
import org.bouncycastle.openssl.PEMException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.deductions.pdsadaptor.utlis.GenerateJWT;

import java.security.PrivateKey;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class GenerateJWTTest {

    @Value("jwtPrivateKey")
    private String jwtPrivateKey;

    @Value("jwtApiKey")
    private String jwtApiKey;

    @Value("accessTokenEndpoint")
    private String accessTokenEndpoint;

    @Value("jwtKeyId")
    private String jwtKeyId;

    @Test
    public void testGetPrivateFromString() throws Exception {
        String keyString = "-----BEGIN RSA PRIVATE KEY-----" + "\n" +
                "MIICXQIBAAKBgQCgDNiws4VaxGcn0tyDyz17/7riG0iMvQ0dEcd3i/TNiKQzRwWG" + "\n" +
                "pZ8V/EG4JestNhmII+zlhUVLXXWG2JhU4T5M7aobCzOOA5gsTOEFuZDejYWP+2u/" + "\n" +
                "GWlwI8xQeKP9JxzSIhGsVsa5pPvlrsTLtvcrt9rberl5oado5PxPjwJhFwIDAQAB" + "\n" +
                "AoGARS+IdEoGOYhxNyvVmzs+Jt4TQS6eHAiVJJ3M5gagGkEZCfmHj/8EWBKlrh7m" + "\n" +
                "HLMoMkulWkpT/BI4fcQfhYGg1h0uJuwdM6sa/etJ6V3d9nRBvRVcFZhq8C6lfCaB" + "\n" +
                "S5ouSmUJFzwkril1utlXD89okSXuWPJvNxJTGwCeDISKS8ECQQDUANChOfm9wadC" + "\n" +
                "80KEgaExDRPhUBvcXrhPj3U/oszhy70Td1FbbC/qARj+xrts640Drnx7al0HbMcV" + "\n" +
                "xbLYxPYRAkEAwUPm3fboCXy5pVsK1NixfpcWfL0AEs0VrzWxPWa8+Lm8c954EVlT" + "\n" +
                "CoTILX1749KdEqA6MbZdmJnTvzD/1dwcpwJAf5YR+MWcUB/IWpltkbM14AA/05xT" + "\n" +
                "eBclEvSCGo8OgGEN5DYtpzh/yXNpqILPbyh/UBTlY5zKadqEIc096gj3EQJBAKaI" + "\n" +
                "dC9fyqIiL3Yk9ThjYM7MMjxaP+3zenP3uDpIhR1uLs1JLf0FE2FE+Zj5QAAYQ/EA" + "\n" +
                "0CR2GECejK968Xi+qpECQQCK5+JuZkYKXHzll9QoW/APlR9/x4ZF6ARbKPS4jGQx" + "\n" +
                "68DBR9bsYxcvKig+IT1HszHOFxq7y1hH4vQsoXKMKJkm" + "\n" +
                "-----END RSA PRIVATE KEY-----";
        GenerateJWT generateJWT = new GenerateJWT();

        PrivateKey key =  generateJWT.getPrivateKey(keyString);
        assertNotNull(key);
    }

    @Test
    public void testEmptyStringException() {
        assertThrows(PEMException.class, () -> {
            GenerateJWT generateJWT = new GenerateJWT();
            generateJWT.getPrivateKey("");
        });
    }

    @Test
    public void testMalformedStringException() {
        assertThrows(PEMException.class, () -> {
            GenerateJWT generateJWT = new GenerateJWT();
            generateJWT.getPrivateKey("test");
        });
    }

    @Test
    public void testCreateSignedJWT() throws Exception {

        String keyString = "-----BEGIN RSA PRIVATE KEY-----" + "\n" +
                "MIICXQIBAAKBgQCgDNiws4VaxGcn0tyDyz17/7riG0iMvQ0dEcd3i/TNiKQzRwWG" + "\n" +
                "pZ8V/EG4JestNhmII+zlhUVLXXWG2JhU4T5M7aobCzOOA5gsTOEFuZDejYWP+2u/" + "\n" +
                "GWlwI8xQeKP9JxzSIhGsVsa5pPvlrsTLtvcrt9rberl5oado5PxPjwJhFwIDAQAB" + "\n" +
                "AoGARS+IdEoGOYhxNyvVmzs+Jt4TQS6eHAiVJJ3M5gagGkEZCfmHj/8EWBKlrh7m" + "\n" +
                "HLMoMkulWkpT/BI4fcQfhYGg1h0uJuwdM6sa/etJ6V3d9nRBvRVcFZhq8C6lfCaB" + "\n" +
                "S5ouSmUJFzwkril1utlXD89okSXuWPJvNxJTGwCeDISKS8ECQQDUANChOfm9wadC" + "\n" +
                "80KEgaExDRPhUBvcXrhPj3U/oszhy70Td1FbbC/qARj+xrts640Drnx7al0HbMcV" + "\n" +
                "xbLYxPYRAkEAwUPm3fboCXy5pVsK1NixfpcWfL0AEs0VrzWxPWa8+Lm8c954EVlT" + "\n" +
                "CoTILX1749KdEqA6MbZdmJnTvzD/1dwcpwJAf5YR+MWcUB/IWpltkbM14AA/05xT" + "\n" +
                "eBclEvSCGo8OgGEN5DYtpzh/yXNpqILPbyh/UBTlY5zKadqEIc096gj3EQJBAKaI" + "\n" +
                "dC9fyqIiL3Yk9ThjYM7MMjxaP+3zenP3uDpIhR1uLs1JLf0FE2FE+Zj5QAAYQ/EA" + "\n" +
                "0CR2GECejK968Xi+qpECQQCK5+JuZkYKXHzll9QoW/APlR9/x4ZF6ARbKPS4jGQx" + "\n" +
                "68DBR9bsYxcvKig+IT1HszHOFxq7y1hH4vQsoXKMKJkm" + "\n" +
                "-----END RSA PRIVATE KEY-----";
        GenerateJWT generateJWT = new GenerateJWT();

        PrivateKey key =  generateJWT.getPrivateKey(keyString);
        Mockito.when(generateJWT.getPrivateKey(Mockito.anyString())).thenReturn(key);
        String token = generateJWT.createSignedJWT();
        assertNotNull(token);
    }
}
