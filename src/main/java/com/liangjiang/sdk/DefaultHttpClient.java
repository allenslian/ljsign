package com.liangjiang.sdk;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class DefaultHttpClient {

    private final String _apiBaseUrl;

    private String _urlPath;

    private String _appId;

    private String _apiKey;

    private String _charset;

    private int _signatureType;

    private String _privateKey;

    private String _publicKey;

    private final ObjectMapper _mapper;

    public DefaultHttpClient(String apiBaseUrl) {
        _mapper = new ObjectMapper();
        _apiBaseUrl = apiBaseUrl;
        loadConfigFromProperties();
    }

    public DefaultHttpClient(String apiBaseUrl, String privateKey, String publicKey) {
        this(apiBaseUrl);
        _privateKey = privateKey;
        _publicKey = publicKey;
    }

    public <T> T get(String url, Object bizContent) throws JsonProcessingException, UnsupportedEncodingException {
        return get(null, url, bizContent);
    }

    public <T> T get(String apiBaseUrl, String url, Object bizContent) throws JsonProcessingException, UnsupportedEncodingException {
        var body = generateRequestBody(bizContent);

        sign(url, body);

        var response = HttpRequest
                .get(buildAbsoluteUri(apiBaseUrl, url, body))
                .header("Content-Type", "application/json", true)
                .execute();
        ObjectNode data = _mapper.readValue(response.body(), ObjectNode.class);
        if (verify(url, data)) {
            return _mapper.readValue(data.get("data").toString(), new TypeReference<>() {});
        }
        throw new IllegalArgumentException("The signature is INVALID!!!");
    }

    public <T> T post(String url, Object bizContent) throws JsonProcessingException, UnsupportedEncodingException {
        return post(null, url, bizContent);
    }

    public <T> T post(String apiBaseUrl, String url, Object bizContent) throws JsonProcessingException, UnsupportedEncodingException {
        var body = generateRequestBody(bizContent);

        sign(url, body);

        var response = HttpRequest
                .post(buildAbsoluteUri(apiBaseUrl, url, null))
                .header("Content-Type", "application/json", true)
                .body(_mapper.writeValueAsString(body))
                .execute();

        ObjectNode data = _mapper.readValue(response.body(), ObjectNode.class);
        if (verify(url, data)) {
            return _mapper.readValue(data.get("data").toString(), new TypeReference<>() {});
        }
        throw new IllegalArgumentException("The signature is INVALID!!!");
    }

    private void loadConfigFromProperties() {
        var resource = ResourceBundle.getBundle("application");
        _appId = resource.getString("app_id");
        _apiKey = resource.getString("api_key");
        _charset = resource.getString("charset");
        _signatureType = Integer.getInteger(resource.getString("signature_type"), 2);
        _privateKey = resource.getString("private_key");
        _publicKey = resource.getString("public_key");
    }

    private ObjectNode generateRequestBody(Object bizContent) {
        if (bizContent == null) {
            throw new IllegalArgumentException("The bizContent is INVALID!!!");
        }

        ObjectNode body = _mapper.createObjectNode();
        body.put("appId", _appId);
        body.put("apiKey", _apiKey);
        body.put("charset", _charset);
        body.putPOJO("data", bizContent);
        body.put("signatureType", _signatureType);
        body.put("timestamp", System.currentTimeMillis()/1000L);
        return body;
    }

    private String buildAbsoluteUri(String apiBaseUrl, String url, ObjectNode body) {
        var builder = UrlBuilder
                .ofHttp(apiBaseUrl == null ? _apiBaseUrl : apiBaseUrl)
                .addPath(_urlPath);
        if (body != null) {
            var iterator = body.fieldNames();
            while (iterator.hasNext()) {
                var k = iterator.next();
                var v = body.get(k);
                if (v.getNodeType() == JsonNodeType.STRING) {
                    builder.addQuery(k, v.asText());
                } else {
                    builder.addQuery(k, v.toString());
                }
            }
        }
        return builder.build();
    }

    private String getStringForSign(String url, ObjectNode body) {
        if (url == null) {
            throw new IllegalArgumentException("url is required!!!");
        }

        var builder = new StringBuilder(128);
        if (!url.startsWith("/")) {
            builder.append("/");
        }

        int pos = url.indexOf("?");
        if (pos > -1) {
            _urlPath = url.substring(0, pos);
            builder.append(_urlPath);
            UrlQuery.of(url.substring(pos), StandardCharsets.UTF_8)
                    .getQueryMap()
                    .forEach((k, v) -> body.put(k.toString(), v.toString()));
        } else {
            _urlPath =url;
            builder.append(_urlPath);
        }

        var fields = new ArrayList<String>();
        var iterator = body.fieldNames();
        while (iterator.hasNext()) {
            fields.add(iterator.next());
        }

        builder.append("?");
        fields.stream().sorted().forEach(k -> {
            if (k.equals("signature")) {
                return;
            }

            builder.append(k).append("=");
            var v = body.get(k);
            if (v.getNodeType() == JsonNodeType.STRING) {
                builder.append(v.asText());
            } else {
                builder.append(v);
            }
            builder.append("&");
        });
        return builder.substring(0, builder.length() - 1);
    }

    private void sign(String url, ObjectNode body) throws UnsupportedEncodingException {
        var text = getStringForSign(url, body);
        var signer = SecureUtil.sign(
                SignAlgorithm.SHA256withRSA,
                _privateKey,
                _publicKey);
        var signature = Base64.encode(signer.sign(text.getBytes(_charset)));
        body.put("signature", signature);
    }

    private boolean verify(String url, ObjectNode body) throws UnsupportedEncodingException {
        var text = getStringForSign(url, body);
        var signature = Base64.decode(body.get("signature").toString());
        var signer = SecureUtil.sign(
                SignAlgorithm.SHA256withRSA,
                _privateKey,
                _publicKey);
        return signer.verify(text.getBytes(_charset), signature);
    }
}
