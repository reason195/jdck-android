package cn.xanderye.android.jdck.util;

import cn.xanderye.android.jdck.entity.QlEnv;
import cn.xanderye.android.jdck.entity.QlInfo;
import com.alibaba.fastjson.JSON;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QinglongUtilTest {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private int port;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();

    @Before
    public void setUp() throws Exception {
        serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        port = serverSocket.getLocalPort();
        serverThread = new Thread(this::serveOnce);
        serverThread.start();
    }

    @After
    public void tearDown() throws Exception {
        serverSocket.close();
        serverThread.join(3000);
    }

    @Test
    public void updatesExistingEnvWhenPanelReturnsId() throws Exception {
        QlEnv env = JSON.parseObject(
                "{\"id\":\"3\",\"name\":\"JD_COOKIE\",\"remarks\":\"reason195\",\"value\":\"pt_key=x;pt_pin=reason195;\"}",
                QlEnv.class);

        assertTrue(QinglongUtil.saveEnv(qlInfo(), env));
        assertEquals("PUT", method.get());
        assertTrue(body.get().contains("\"id\":\"3\""));
    }

    @Test
    public void createsEnvWhenEnvHasNoId() throws Exception {
        QlEnv env = JSON.parseObject(
                "{\"name\":\"JD_COOKIE\",\"value\":\"pt_key=x;pt_pin=reason195;\"}",
                QlEnv.class);

        assertTrue(QinglongUtil.saveEnv(qlInfo(), env));
        assertEquals("POST", method.get());
        assertTrue(body.get().startsWith("["));
    }

    private QlInfo qlInfo() {
        QlInfo qlInfo = new QlInfo();
        qlInfo.setAddress("http://127.0.0.1:" + port);
        qlInfo.setToken("test-token");
        return qlInfo;
    }

    private void serveOnce() {
        try (Socket socket = serverSocket.accept()) {
            InputStream inputStream = socket.getInputStream();
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            int[] tail = new int[4];
            while (true) {
                int b = inputStream.read();
                if (b == -1) {
                    return;
                }
                header.write(b);
                tail[0] = tail[1];
                tail[1] = tail[2];
                tail[2] = tail[3];
                tail[3] = b;
                if (tail[0] == '\r' && tail[1] == '\n' && tail[2] == '\r' && tail[3] == '\n') {
                    break;
                }
            }
            String[] lines = new String(header.toByteArray(), StandardCharsets.ISO_8859_1).split("\r\n");
            method.set(lines[0].split(" ")[0]);
            int contentLength = 0;
            for (String line : lines) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                }
            }
            byte[] bodyBytes = new byte[contentLength];
            int read = 0;
            while (read < contentLength) {
                int count = inputStream.read(bodyBytes, read, contentLength - read);
                if (count == -1) {
                    break;
                }
                read += count;
            }
            body.set(new String(bodyBytes, 0, read, StandardCharsets.UTF_8));

            byte[] response = "{\"code\":200}".getBytes(StandardCharsets.UTF_8);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(("HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + response.length + "\r\n"
                    + "Connection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            outputStream.write(response);
            outputStream.flush();
        } catch (IOException ignored) {
        }
    }
}
