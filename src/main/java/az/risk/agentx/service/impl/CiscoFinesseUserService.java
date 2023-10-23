package az.risk.agentx.service.impl;

import az.risk.agentx.dto.AgentDto;
import az.risk.agentx.dto.ChangeStateDto;
import az.risk.agentx.exception.AgentStateException;
import az.risk.agentx.exception.FinesseApiRequestFailedException;
import az.risk.agentx.model.ReasonCode;
import az.risk.agentx.util.XmlToJavaConverter;
import az.risk.agentx.model.user.AgentState;
import az.risk.agentx.model.user.User;
import az.risk.agentx.service.UserService;
import az.risk.agentx.util.xmpp.XmppConnectionFactory;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.Level;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


@Log4j2
@Service
@RequiredArgsConstructor
public class CiscoFinesseUserService implements UserService {

    private static final String FINESSE_USER_API_URL = "https://uccx01.zbaz.local:8445/finesse/api/User/%s";

    private final static SSLContext sslContext;

    private final XmppConnectionFactory xmppConnectionFactory;

    static {
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("SSLContext loadTrust Material Failed");
            log.catching(Level.ERROR, e);
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }
    }

    @Override
    public User getUser(String username, String password) {

        log.trace("Get user init");

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {
            HttpGet httpGet = new HttpGet(FINESSE_USER_API_URL.formatted(username));

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                var statusCode = response.getStatusLine().getStatusCode();

                log.info("Status code from Finesse Get user API is {}", statusCode);

                if (statusCode == 200) {
                    var entity = response.getEntity();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                        StringBuilder responseStr = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseStr.append(line);
                        }
                        log.info("Response body is {}", responseStr);

                        var node = XmlToJavaConverter.parseXmlToJsonNode(responseStr.toString());
                        var rolesNode = node != null ? node.get("roles").get("role").iterator() : null;
                        var roles = new ArrayList<String>();
                        var pendingState = node != null ? node.get("pendingState") : null;
                        var state = pendingState != null && !pendingState.asText().isEmpty() ? pendingState : node != null ? node.get("state") : null;
                        var extension = node != null ? node.get("extension").asInt(0) : 0;

                        while (rolesNode != null && rolesNode.hasNext()) {
                            roles.add(rolesNode.next().asText());
                        }
                        var user = new User(username, password, roles, AgentState.valueOf(state != null ? state.asText() : null), extension);
                        log.trace("Returning user {}", user);
                        return user;
                    } catch (Exception e) {
                        log.error("Exception occurred while fetching user from Finesse : {}", e.getMessage());
                        log.catching(Level.ERROR, e);
                        log.trace("Throwing RuntimeException");
                        throw new RuntimeException(e);
                    }
                }

            } catch (IOException e) {
                log.error(e.getMessage());
                log.catching(Level.ERROR, e);
                log.trace("Throwing RuntimeException");
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
            log.catching(Level.ERROR, e);
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public int signIn(int extension) {

        String xmlPayload = "<User><state>LOGIN</state><extension>%s</extension></User>".formatted(extension);

        return changeAgentState(xmlPayload);

    }

    @Override
    public int singOut(String reasonCodeId) {
        String xmlPayload = "<User><state>LOGOUT</state><reasonCodeId>%s</reasonCodeId></User>".formatted(reasonCodeId);
        return changeAgentState(xmlPayload);
    }


    @Override
    public AgentDto changeState(ChangeStateDto changeStateDto) {

        var state = changeStateDto.state();
        var reasonCodeId = changeStateDto.reasonCodeId();


        var authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        log.trace("Logged In user is {}", authUser);

        var user = getUser(authUser.getUsername(), authUser.getPassword());
        if (user.getState().equals(AgentState.LOGOUT) || !xmppConnectionFactory.isConnected(user.getUsername())) {

            throw new AgentStateException("Agent is not connected. Please connect first");
        }

        if (state.equals(AgentState.LOGIN.name()) || state.equals(AgentState.LOGOUT.name())) {
            throw new FinesseApiRequestFailedException("Invalid State specified for user");
        }

        String xmlPayload = "<User><state>%s</state><reasonCodeId>%s</reasonCodeId></User>".formatted(state, reasonCodeId);
        changeAgentState(xmlPayload);

        var userAfterStateChange = getUser(authUser.getUsername(), user.getPassword());

        return new AgentDto(userAfterStateChange.getUsername(), userAfterStateChange.getState());
    }

    @Override
    public List<ReasonCode> getReasonCodeListByCategory(String category) {

        var authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        log.trace("Logged In user is {}", authUser);

        var user = getUser(authUser.getUsername(), authUser.getPassword());
        if (user.getState().equals(AgentState.LOGOUT) || !xmppConnectionFactory.isConnected(user.getUsername())) {

            throw new AgentStateException("Agent is not connected. Please connect first");
        }

        var reasonCodeList = new ArrayList<ReasonCode>();

        log.trace("Get reason code list init");

        log.trace("Logged In user is {}", authUser);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(authUser.getUsername(), authUser.getPassword()));
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {
            var url = FINESSE_USER_API_URL.formatted(authUser.getUsername() + "/ReasonCodes?category=" + category);

            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

                var statusCode = response.getStatusLine().getStatusCode();

                log.info("Status code from Finesse Get user API is {}", statusCode);

                var entity = response.getEntity();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                    if (statusCode == 200) {
                        StringBuilder responseStr = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseStr.append(line);
                        }
                        log.info("Response body is {}", responseStr);

                        var node = XmlToJavaConverter.parseXmlToJsonNode(responseStr.toString());
                        var reasonCodeNode = node != null ? node.get("ReasonCode") : null;
                        var reasonCodeNodeIterator=reasonCodeNode!=null?reasonCodeNode.iterator():null;
                        if (reasonCodeNodeIterator==null) return new ArrayList<>();
                        var reasonCodes = new ArrayList<JsonNode>();

                        while (reasonCodeNodeIterator.hasNext()) {
                            reasonCodes.add(reasonCodeNodeIterator.next());
                        }
                        reasonCodes.forEach(reasonCode -> {
                                    var uri=reasonCode.get("uri").asText();
                                    var splitUri = uri.split("/");
                                    var reasonCodeId=splitUri[4];
                                    reasonCodeList.add(new ReasonCode(reasonCode.get("category").asText(),reasonCodeId, reasonCode.get("label").asText()));
                                });

                        log.trace("Returning Reason code list  with size {}", reasonCodeList.size());
                    } else {
                        var errorMessage = getErrorMessage(reader, "ErrorData");
                        throw new FinesseApiRequestFailedException(errorMessage);
                    }
                }

                return reasonCodeList;
            }
        } catch (IOException e) {
            log.error("Exception occurred while fetching Reason Codes for category {} from Finesse : {}", category, e.getMessage());
            log.catching(Level.ERROR, e);
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }


    }

    private static int changeAgentState(String xmlPayload) {

        log.trace("Change agent state init");

        log.trace("Payload is {}", xmlPayload);

        var authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.trace("Logged In user is {}", authUser);


        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(authUser.getUsername(), authUser.getPassword()));
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {
            HttpPut httpPut = new HttpPut(FINESSE_USER_API_URL.formatted(authUser.getUsername()));

            // Add required headers for sign-in
            httpPut.addHeader(HttpHeaders.ACCEPT, "application/xml");
            httpPut.addHeader(HttpHeaders.CONTENT_TYPE, "application/xml");

            // Create the XML payload for sign-in

            httpPut.setEntity(new StringEntity(xmlPayload));
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                log.trace("returning API response code {}", response.getStatusLine().getStatusCode());
                var statusCode = response.getStatusLine().getStatusCode();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    if (statusCode != 202) {
                        var errorMessage = getErrorMessage(reader, "ErrorMessage");
                        throw new FinesseApiRequestFailedException(errorMessage);
                    }
                }
                return response.getStatusLine().getStatusCode();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            log.catching(Level.ERROR, e);
            log.trace("Throwing RuntimeException");
            throw new RuntimeException(e);
        }
    }

    private static String getErrorMessage(BufferedReader reader, String errorField) {
        StringBuilder responseStr = new StringBuilder();

        String line;
        while (true) {
            try {
                if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                log.error(e.getMessage());
                log.catching(Level.ERROR, e);
                log.trace("Throwing RuntimeException");
                throw new RuntimeException(e);
            }

            responseStr.append(line);
        }
        log.trace(responseStr);
        var node = XmlToJavaConverter.parseXmlToJsonNode(responseStr.toString());
        var ApiErrorNode = node != null ? node.get("ApiError") : null;
        var errorMessageNode = ApiErrorNode != null ? ApiErrorNode.get(errorField) : null;
        return errorMessageNode != null ? errorMessageNode.asText() : "Failed";
    }

}
