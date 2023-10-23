package az.risk.agentx.service.impl;

import az.risk.agentx.dto.CallDto;
import az.risk.agentx.util.XmlToJavaConverter;
import az.risk.agentx.exception.*;
import az.risk.agentx.model.user.AgentState;
import az.risk.agentx.model.user.User;
import az.risk.agentx.service.CallService;
import az.risk.agentx.service.UserService;
import az.risk.agentx.util.xmpp.XmppConnectionFactory;
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

@Log4j2
@Service
@RequiredArgsConstructor
public class CiscoFinesseDialogService implements CallService {

    private final XmppConnectionFactory xmppConnectionFactory;
    private final UserService userService;

    private static final String FINESSE_DIALOG_API_URL = "https://uccx01.zbaz.local:8445/finesse/api/Dialog/%s";

    private static final SSLContext sslContext;

    static {
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void makeCall(String toAddress) {

    }

    @Override
    public CallDto answerCall(String dialogId) {

        var loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        var user = userService.getUser(loggedInUser.getUsername(), loggedInUser.getPassword());
        if (user.getState().equals(AgentState.LOGOUT) || !xmppConnectionFactory.isConnected(user.getUsername())) {
            throw new AgentStateException("Agent is not connected. Please connect first");
        }

        var xmlPayload = """
                <Dialog>
                    <targetMediaAddress>%s</targetMediaAddress>
                    <requestedAction>ANSWER</requestedAction>
                </Dialog>""".formatted(user.getExtension());

        takeActionOnParticipant(dialogId, loggedInUser.getUsername(), loggedInUser.getPassword(), xmlPayload);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        var call = getDialog(dialogId, loggedInUser.getUsername(), loggedInUser.getPassword());

        if (call == null) {
            throw new InvalidCallActionException("Call not found");
        }

        if (!call.state().equals("ACTIVE")) {
            throw new InvalidCallActionException("Failed to answer to call");
        }

        return call;

    }

    @Override
    public void transferCall(String dialogId, String toAddress) {

    }

    @Override
    public void conferenceCall(String dialogId, String toAddress) {

    }


    @Override
    public CallDto endCall(String dialogId) {

        var loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        var user = userService.getUser(loggedInUser.getUsername(), loggedInUser.getPassword());
        if (user.getState().equals(AgentState.LOGOUT) || !xmppConnectionFactory.isConnected(user.getUsername())) {
            throw new AgentStateException("Agent is not connected. Please connect first");
        }

        var xmlPayload = """
                <Dialog>
                    <targetMediaAddress>%s</targetMediaAddress>
                    <requestedAction>DROP</requestedAction>
                </Dialog>""".formatted(user.getExtension());

        takeActionOnParticipant(dialogId, loggedInUser.getUsername(), loggedInUser.getPassword(), xmlPayload);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        var call = getDialog(dialogId, loggedInUser.getUsername(), loggedInUser.getPassword());

        if (call != null) {
            throw new InvalidCallActionException("Failed to end call");
        }
        return new CallDto(dialogId, "0", "0", "Ended");
    }

    private CallDto getDialog(String dialogId, String username, String password) {

        log.trace("Get dialog init with id {}", dialogId);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {
            HttpGet httpGet = new HttpGet(FINESSE_DIALOG_API_URL.formatted(dialogId));

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                var statusCode = response.getStatusLine().getStatusCode();

                log.info("Status code from Finesse Get dialog API is {}", statusCode);

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
                        var fromAddressNode = node != null ? node.get("fromAddress") : null;
                        var toAddressNode = node != null ? node.get("toAddress") : null;
                        var stateNode = node != null ? node.get("state") : null;

                        var fromAddress = fromAddressNode != null ? fromAddressNode.asText() : "";
                        var toAddress = toAddressNode != null ? toAddressNode.asText() : "";
                        var state = stateNode != null ? stateNode.asText() : "";

                        return new CallDto(dialogId, fromAddress, toAddress, state);

                    } catch (Exception e) {
                        log.error("Exception occurred while fetching dialog from Finesse : {}", e.getMessage());
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


    private void takeActionOnParticipant(String dialogId, String username, String password, String xmlPayload) {

        log.trace("Take Action on participant init with payload {}, and dialog id {}", xmlPayload, dialogId);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                .build()) {

            HttpPut httpPut = new HttpPut(FINESSE_DIALOG_API_URL.formatted(dialogId));

            httpPut.addHeader(HttpHeaders.ACCEPT, "application/xml");
            httpPut.addHeader(HttpHeaders.CONTENT_TYPE, "application/xml");

            httpPut.setEntity(new StringEntity(xmlPayload));
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                var statusCode = response.getStatusLine().getStatusCode();
                log.trace("Status code from Finesse Take action on participant API {}", statusCode);
                if (statusCode != 202) {
                    var entity = response.getEntity();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                        String message = getErrorMessage(reader);
                        log.trace("Errormessage is {}", message);
                        log.trace("Throwing FinesseApiRequestFailedException");
                        throw new FinesseApiRequestFailedException(message);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        log.catching(Level.ERROR, e.getCause());
                        log.trace("Throwing RuntimeException");
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            log.catching(Level.ERROR, e.getCause());
            log.trace("Throwing RuntimeException");
        }

    }

    private static String getErrorMessage(BufferedReader reader) throws IOException {
        StringBuilder responseStr = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseStr.append(line);
        }
        var node = XmlToJavaConverter.parseXmlToJsonNode(responseStr.toString());
        var ApiErrorNode = node != null ? node.get("ApiError") : null;
        var errorMessageNode = ApiErrorNode != null ? ApiErrorNode.get("ErrorMessage") : null;
        return errorMessageNode != null ? errorMessageNode.asText() : "Call action failed";
    }
}
