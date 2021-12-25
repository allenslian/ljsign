package com.liangjiang.sdk;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class SignTest {
    private final String privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDegJFZ/npN003R" +
            "6WfxfEJA/oGIxB3HXE6uqosrcsnu69FmY07gYedZavoMEulSZU0dbOg0JpYlmHxM" +
            "YqckcPfYFO9Ms2aBYB1KqvRyruw8yc2aE4eS4A6K5NouMDkIl//gkuRFlw0m/IjI" +
            "ryGkMGgVasJ4H9OhBJFotYHKm1+EvfrX+1aTBS//gZKUDKj3uP59WYngo3N//u82" +
            "ZzdRIhLVSkM+qny+3F04cN36aP9d7+Xuwm6JTye0meJSfRYAKgsg5gPfgHi315Np" +
            "cy51Ha5ZukbQN8Xz3axIQuiBxJ8K27B+ZXhnzwIPdzEmQOXhIxtER5fGl9Q2bAZ2" +
            "TqTrJKkTAgMBAAECggEBANIr3aCFaV+0FhEPCVkox6h+/zqCDaLwBX6lp5nd7vLZ" +
            "G848RhhbQl3LV/ApuO1UtPfzlI8WV8Ohet/oW/tnHiWk+LmZyFyyvlh2jTr7EjkF" +
            "YkmE4w4QyIvYSmNQt77vXQrzld5KJWlLJ6VxEDT4aIi6dIFlhtDU8MlYkgYgp6RI" +
            "0NXyBHyKaX2CqvGyAJKoy13qV6ZxlZY7dKkO9yBc486CPl2+zXDOJxsp3btvfyIw" +
            "SUHBfdRSDCiUoCwiHvcFRjKh0zlRlxxp53NbJ+TdB9WufbasWRgw1O1dzP75A6gy" +
            "Gcx2H96sovfuu+fX/oyvnZGHeggyDJKxvUG9/J+EXCkCgYEA9qbV7eRIsrwXpZmf" +
            "Q0YTgRXOjY8AXhHWGfSvjx+o2ZR6o9mD8OdJ3/XobH8zymI+d2pPAgeNepSEmAYg" +
            "E7yszCLgWxXz4BSKJEj8CVkzLGUXn/dfO/w9Y/Rp/H/OVPsXRey6L8euV4OY7j+/" +
            "WTxfteXuGR5t5mZ6uaxVmTj1xr8CgYEA5u9rT7gbge63dTVLZjZWJPmVrFd81NhK" +
            "GCCsWJPpal4G9nDHTcakyeY9bIra6biENJnzomh09YVH2asbh20Rw0ZbniE+njIh" +
            "OTJHL6EcyWUx0hoyY5jcPJLxGZODvKsXu0jy7XZN9U6tqtLwGkswxGP0+3fCSx6i" +
            "Gozt/YazJq0CgYBspl9UVJwlh6+O9hXu536N/VIoj1alGYxLkLqI/HQ/rCU96gIx" +
            "62BidIt2x63Dt/U2WzEEftk2pxeldAVLTHB919smpIvyKHoVs8S6RX3CT6HRiIF+" +
            "BgXw8uiBHreAPb8pwTbe90H4MPL7+D7NC0hJ80gn7nyQ4pGrGpv2S94QNwKBgFh5" +
            "WU+NZVx+LGMHK//YyWAZDrKcQgL5akmcGutEn6RUqa44vuKZPADt6JJxEDWCr4PH" +
            "h4OtKUH28fP+jelwa+G4TtliOy84XWogTQ1+WB0AW/n2d1+Y7Kd4VE5MKahalBj4" +
            "dblsHanhnr9XWQ15yRE0imLdGt7UuZ4aaAM9W91xAoGAXlYlsR5tT4AISpPLD4M/" +
            "K27y5Ymn6QpTEgtumsAHRishsjbC/Oct3W2A8EGvqchIK/y13oXr9a3j6Czns9ir" +
            "14B/ON6XKHAD/R+3VQ91ekpnZc6uSt4ZAuXM7ExwOV5f5+BXS9SVRG5JtC0QaSqR" +
            "l4nbxU+z5CZsQS6jZmH5/Oo=";
    private final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3oCRWf56TdNN0eln8XxC" +
            "QP6BiMQdx1xOrqqLK3LJ7uvRZmNO4GHnWWr6DBLpUmVNHWzoNCaWJZh8TGKnJHD3" +
            "2BTvTLNmgWAdSqr0cq7sPMnNmhOHkuAOiuTaLjA5CJf/4JLkRZcNJvyIyK8hpDBo" +
            "FWrCeB/ToQSRaLWByptfhL361/tWkwUv/4GSlAyo97j+fVmJ4KNzf/7vNmc3USIS" +
            "1UpDPqp8vtxdOHDd+mj/Xe/l7sJuiU8ntJniUn0WACoLIOYD34B4t9eTaXMudR2u" +
            "WbpG0DfF892sSELogcSfCtuwfmV4Z88CD3cxJkDl4SMbREeXxpfUNmwGdk6k6ySp" +
            "EwIDAQAB";

    @Test
    public void TestSignature() {
        String plain = "hello";
        var signer = SecureUtil.sign(
                SignAlgorithm.SHA256withRSA,
                privateKey,
                publicKey);
        var signature = Base64.encode(signer.sign(plain.getBytes(StandardCharsets.UTF_8)));
        Assertions.assertNotEquals("", signature);
        Assertions.assertTrue(signer.verify(plain.getBytes(StandardCharsets.UTF_8), Base64.decode(signature)));
    }

    @Test
    public void TestHttpGet() throws UnsupportedEncodingException, JsonProcessingException {
        var req = new Result();
        req.setCharset("utf-8");
        req.setData("ok");
        var client = new DefaultHttpClient("http://localhost:5000");
        Result result = client.get("/api/v1/hello?greet=%E4%BD%A0%E5%A5%BD", req);
    }

    @Test
    public void TestHttpPost() throws UnsupportedEncodingException, JsonProcessingException {
        var req = new Result();
        req.setCharset("utf-8");
        req.setData("ok");
        var client = new DefaultHttpClient("http://localhost:5000");
        Result result = client.post("/api/v1/hello?greet=%E4%BD%A0%E5%A5%BD", req);
    }

}
