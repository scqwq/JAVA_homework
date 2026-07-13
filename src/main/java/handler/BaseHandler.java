package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class BaseHandler implements HttpHandler {
    protected Map<String, String> queryParams(HttpExchange exchange) {
        return parseEncodedPairs(exchange.getRequestURI().getRawQuery());
    }

    protected Map<String, String> formParams(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return parseEncodedPairs(body);
        }
    }

    protected void ensureMethod(HttpExchange exchange, String expectedMethod) {
        if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new IllegalArgumentException("不支持的请求方法: " + exchange.getRequestMethod());
        }
    }

    protected long requireLong(Map<String, String> params, String key, String label) {
        String value = requireText(params, key, label);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + "必须是数字");
        }
    }

    protected int requireInt(Map<String, String> params, String key, String label) {
        String value = requireText(params, key, label);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + "必须是数字");
        }
    }

    protected String requireText(Map<String, String> params, String key, String label) {
        String value = params.getOrDefault(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value;
    }

    protected void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    protected void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    protected String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    protected String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private Map<String, String> parseEncodedPairs(String source) {
        Map<String, String> values = new LinkedHashMap<>();
        if (source == null || source.isBlank()) {
            return values;
        }
        for (String pair : source.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
