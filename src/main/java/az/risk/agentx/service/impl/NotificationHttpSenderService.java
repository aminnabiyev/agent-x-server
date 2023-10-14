package az.risk.agentx.service.impl;

import az.risk.agentx.model.Call;
import az.risk.agentx.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.Level;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Service
@Primary
@Log4j2
public class NotificationHttpSenderService implements NotificationService {

    private static final String API_URL = "https://tdevappsback.ziraatbank.az:8184/api/CiscoWebHook/%s";

    private static final SSLContext sslContext;

    static {
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error(e.getMessage());
            log.catching(Level.ERROR, e.getCause());
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void newCall(String sessionId, String incomingNumber, String startDate, String operator) {

        log.trace("Send new Incoming call event with session id : {}", sessionId);


        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {

            var call = new Call(sessionId, incomingNumber, startDate, operator);

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(call);

            HttpPost httpPost = new HttpPost(API_URL.formatted("NewCall"));
            httpPost.setEntity(new StringEntity(json));

            httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                var statusCode = response.getStatusLine().getStatusCode();
                var entity = response.getEntity();
                StringBuilder responseStr = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseStr.append(line);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    log.catching(Level.ERROR, e.getCause());
                    log.trace("Throwing Runtime exception");
                    throw new RuntimeException(e);
                }
                log.trace("Ziraat New call api response code is {}", statusCode);
                log.trace("Ziraat New call api response body is {}", responseStr);
                if (statusCode != 200) {
                    log.trace("Ziraat NewCall Api response is not 200");
                    log.trace("Throwing RuntimeException");
                    throw new RuntimeException("Can not send new call notification");
                }

            } catch (IOException e) {
                log.error(e.getMessage());
                log.catching(Level.ERROR, e.getCause());
                log.trace("Throwing RuntimeException");
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
            log.catching(Level.ERROR, e.getCause());
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void missedCall(String sessionId) {
        String requestBody = """
                { "sessionId": %s }""".formatted(sessionId);

        log.trace("Send Missed call event with session id : {}", sessionId);


        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {

            HttpPost httpPost = new HttpPost(API_URL.formatted("MissedCall"));
            httpPost.setEntity(new StringEntity(requestBody));

            httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                var statusCode = response.getStatusLine().getStatusCode();
                var entity = response.getEntity();
                StringBuilder responseStr = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseStr.append(line);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    log.catching(Level.ERROR, e.getCause());
                    log.trace("Throwing RuntimeException");
                    throw new RuntimeException(e);
                }
                log.trace("Ziraat Missed call api response code is {}", statusCode);
                log.trace("Ziraat Missed call api response body is {}", responseStr);
                if (statusCode != 200) {
                    log.trace("Ziraat Missed Api response is not 200");
                    log.trace("Throwing RuntimeException");
                    throw new RuntimeException("Can not send Missed call notification");
                }

            } catch (IOException e) {
                log.error(e.getMessage());
                log.catching(Level.ERROR, e.getCause());
                log.trace("Throwing RuntimeException");
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
            log.catching(Level.ERROR, e.getCause());
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void answeredCall(String sessionId) {
        String requestBody = """
                {
                "sessionId": %s
                }""".formatted(sessionId);

        log.trace("Send Answered event with session id : {}", sessionId);


        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {

            HttpPost httpPost = new HttpPost(API_URL.formatted("AnsweredCall"));
            httpPost.setEntity(new StringEntity(requestBody));

            httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                var statusCode = response.getStatusLine().getStatusCode();
                var entity = response.getEntity();
                StringBuilder responseStr = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseStr.append(line);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    log.catching(Level.ERROR, e.getCause());
                    log.trace("Throwing RuntimeException");
                    throw new RuntimeException(e);
                }
                log.trace("Ziraat Answered call api response code is {}", statusCode);
                log.trace("Ziraat Answered call api response body is {}", responseStr);

                if (statusCode != 200) {
                    log.trace("Ziraat Answered Api response is not 200");
                    log.trace("Throwing RuntimeException");
                    throw new RuntimeException("Can not send Answered call notification");
                }

            } catch (IOException e) {
                log.error(e.getMessage());
                log.catching(Level.ERROR, e.getCause());
                log.trace("Throwing RuntimeException");
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
            log.catching(Level.ERROR, e.getCause());
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void endedCall(String sessionId) {
        String requestBody = """
                {
                "sessionId": %s
                }""".formatted(sessionId);

        log.trace("Send Ended call event with session id : {}", sessionId);


        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {

            HttpPost httpPost = new HttpPost(API_URL.formatted("EndedCall"));
            httpPost.setEntity(new StringEntity(requestBody));

            httpPost.addHeader(HttpHeaders.ACCEPT, "application/json");
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                var statusCode = response.getStatusLine().getStatusCode();
                var entity = response.getEntity();
                StringBuilder responseStr = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseStr.append(line);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    log.catching(Level.ERROR, e.getCause());
                    log.trace("Throwing RuntimeException");
                    throw new RuntimeException(e);
                }
                log.trace("Ziraat ended call api response code is {}", statusCode);
                log.trace("Ziraat ended call api response body is {}", responseStr);
                if (statusCode != 200) {
                    log.trace("Ziraat Ended Api response is not 200");
                    log.trace("Throwing RuntimeException");
                    throw new RuntimeException("Can not send Ended call notification");
                }

            } catch (IOException e) {
                log.error(e.getMessage());
                log.catching(Level.ERROR, e.getCause());
                log.trace("Throwing RuntimeException");
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
            log.catching(Level.ERROR, e.getCause());
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }
    }
}

