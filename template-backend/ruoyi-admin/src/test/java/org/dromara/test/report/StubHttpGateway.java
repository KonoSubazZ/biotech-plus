package org.dromara.test.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * 测试用极简 HTTP 桩：按「路径 → 响应」返回 JSON，并记录收到的请求（供断言报文内容）。
 * 只跑在本机回环、单测结束即关；响应带 Connection: close，一个请求一个连接。
 *
 * @author <你的名字>
 */
final class StubHttpGateway implements AutoCloseable {

    /** 一次收到的请求 */
    record Recorded(String path, String body) {
    }

    private final ServerSocket server;
    private final Map<String, Function<Recorded, String>> responses = new ConcurrentHashMap<>();
    private final Map<String, byte[]> rawBodies = new ConcurrentHashMap<>();
    private final List<Recorded> received = new CopyOnWriteArrayList<>();
    private final Thread acceptor;

    StubHttpGateway() throws IOException {
        this.server = new ServerSocket(0);
        this.acceptor = new Thread(this::serve);
        this.acceptor.setDaemon(true);
        this.acceptor.start();
    }

    /** 按路径注册响应（响应体是 JSON 文本；调用方可用 Function 依请求体动态返回） */
    void respond(String path, String json) {
        responses.put(path, request -> json);
    }

    void respond(String path, Function<Recorded, String> json) {
        responses.put(path, json);
    }

    /** 按路径注册「二进制」响应（下载类接口用）：body 原样发出，Content-Type: application/pdf */
    void respondBytes(String path, byte[] bytes) {
        rawBodies.put(path, bytes);
        responses.put(path, request -> "");
    }

    /** 收到的请求（按先后） */
    List<Recorded> received() {
        return received;
    }

    /** 某路径片段收到几次（与响应注册同口径：contains 匹配，base64 段在 URL 中间，不能用 startsWith） */
    long count(String path) {
        return received.stream().filter(r -> r.path().contains(path)).count();
    }

    /** 桩地址，如 http://localhost:34567 */
    String baseUrl() {
        return "http://localhost:" + server.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        server.close();
    }

    private void serve() {
        while (!server.isClosed()) {
            try (Socket socket = server.accept()) {
                Recorded request = read(socket.getInputStream());
                received.add(request);
                respond(socket.getOutputStream(), request);
            } catch (IOException e) {
                return;
            }
        }
    }

    /** 读请求行 + 头 +（按 Content-Length）body */
    private Recorded read(InputStream in) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = in.read()) != -1) {
            head.write(current);
            if (previous == '\r' && current == '\n') {
                break;
            }
            previous = current;
        }
        String requestLine = head.toString(StandardCharsets.UTF_8).split("\r\n")[0];
        String path = requestLine.split(" ")[1];

        int contentLength = 0;
        String line;
        do {
            line = readLine(in);
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
            }
        } while (!line.isEmpty());

        byte[] body = new byte[contentLength];
        int read = 0;
        while (read < contentLength) {
            int count = in.read(body, read, contentLength - read);
            if (count < 0) {
                break;
            }
            read += count;
        }
        return new Recorded(path, new String(body, StandardCharsets.UTF_8));
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder line = new StringBuilder();
        int previous = -1;
        int current;
        while ((current = in.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                line.setLength(line.length() - 1);
                return line.toString();
            }
            line.append((char) current);
            previous = current;
        }
        return line.toString();
    }

    private void respond(OutputStream out, Recorded request) throws IOException {
        byte[] raw = rawBodies.entrySet().stream()
            .filter(entry -> request.path().contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
        if (raw != null) {
            String rawHeader = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/pdf\r\n"
                + "Content-Length: " + raw.length + "\r\n"
                + "Connection: close\r\n\r\n";
            out.write(rawHeader.getBytes(StandardCharsets.UTF_8));
            out.write(raw);
            out.flush();
            return;
        }
        Function<Recorded, String> supplier = responses.entrySet().stream()
            .filter(entry -> request.path().contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(r -> "{\"code\":\"error.notFound\"}");
        String json = supplier.apply(request);
        int status = json.contains("\"__status401__\"") ? 401 : 200;
        byte[] body = json.replace("\"__status401__\"", "\"error.permission.accessTokenInvalid\"").getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + status + (status == 200 ? " OK" : " Unauthorized") + "\r\n"
            + "Content-Type: application/json;charset=UTF-8\r\n"
            + "Content-Length: " + body.length + "\r\n"
            + "Connection: close\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }
}
