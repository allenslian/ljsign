package com.liangjiang.sdk;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class JsonTest {
    @Test
    public void testParseJson() throws JsonProcessingException {
        String json = "{\r\n \"charset\": \"utf-8\", \r\n \"data\": {\r\n \"msg\": \"error\" \r\n} \r\n}";
        var mapper = new ObjectMapper();
        var body01 = mapper.readValue(json, JsonNode.class);
        Assertions.assertNotEquals("{\r\n" +
                " \"msg\": \"error\" " +
                "\r\n}", body01.get("data").toString());
        var body02 = mapper.readValue(json, Result.class);
        Assertions.assertEquals("{\r\n" +
                " \"msg\": \"error\" \r\n" +
                "}", body02.getData());
    }

    @Test
    public void testParseJsonToMap() throws JsonProcessingException {
        String json = "{\r\n \"charset\": \"utf-8\", \r\n \"data\": {\r\n \"msg\": \"error\" \r\n} \r\n}";
        var mapper = new ObjectMapper();
        Map<String, JsonNode> result = mapper.readValue(json, new TypeReference<>() {});
        Assertions.assertEquals("{\"msg\":\"error\"}", result.get("data").toString());
    }

    @Test
    public void testToJson() throws JsonProcessingException {
        var result = new Result();
        result.setCharset("utf-8");
        result.setData("error");
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(result);
        Assertions.assertEquals("{\"charset\":\"utf-8\",\"data\":\"error\"}", json);

        var results = new ArrayList<Result>();
        results.add(result);
        results.add(result);
        json = mapper.writeValueAsString(results);
        Assertions.assertEquals("[{\"charset\":\"utf-8\",\"data\":\"error\"},{\"charset\":\"utf-8\",\"data\":\"error\"}]", json);
    }

    @Test
    public void testUrl() {
        var b = UrlBuilder.of("/api/v1/hello")
                .setQuery(UrlQuery.of("?greet=你好&state=1&data={\"success\":true}&array=[{\"charset\":\"utf-8\",\"data\":\"error\"},{\"charset\":\"utf-8\",\"data\":\"error\"}]", StandardCharsets.UTF_8));
        Assertions.assertEquals("/api/v1/hello", b.getPath().toString());
        Assertions.assertEquals("greet=你好&state=1&data={\"success\":true}&array=[{\"charset\":\"utf-8\",\"data\":\"error\"},{\"charset\":\"utf-8\",\"data\":\"error\"}]", b.getQuery().toString());
        var node = new ObjectMapper().createObjectNode();
        b.getQuery().getQueryMap().forEach((k, v) -> node.put(k.toString(), v.toString()));
        Assertions.assertEquals("你好", node.get("greet").asText());
        Assertions.assertEquals(1, node.get("state").asInt());
        Assertions.assertEquals("{\"success\":true}", node.get("data").asText());
        Assertions.assertEquals("[{\"charset\":\"utf-8\",\"data\":\"error\"},{\"charset\":\"utf-8\",\"data\":\"error\"}]", node.get("array").asText());
    }
}
