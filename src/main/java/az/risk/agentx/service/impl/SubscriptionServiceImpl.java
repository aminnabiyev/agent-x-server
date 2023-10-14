package az.risk.agentx.service.impl;

import az.risk.agentx.dto.AgentDto;
import az.risk.agentx.exception.AgentStateException;
import az.risk.agentx.exception.FinesseApiRequestFailedException;
import az.risk.agentx.model.user.AgentState;
import az.risk.agentx.model.user.User;
import az.risk.agentx.util.xmpp.XmppConnectionFactory;
import az.risk.agentx.service.NotificationService;
import az.risk.agentx.service.UserService;
import az.risk.agentx.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserService userService;
    private final NotificationService callService;
    private final XmppConnectionFactory xmppConnectionFactory;


    @Override
    public AgentDto subscribe(int extension) {

        log.trace("Subscribe called with extension {}", extension);
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var username = user.getUsername();
        var password = user.getPassword();
        var state = user.getState();

        xmppConnectionFactory.connect(username, password, extension);

        if (state.equals(AgentState.LOGOUT)) {
            if (!signInToFinesse(extension)) {
                log.trace("Finesse sign in failed");
                log.trace("Disconnection xmpp connection");
                xmppConnectionFactory.disconnect(username);
                log.trace("Throwing FinesseApiRequestFailedException");
                throw new FinesseApiRequestFailedException("Finesse sign in failed");
            }
        }

        return checkAndGetIfUserLoggedInAndExtensionIsMatching(username, password, extension);


    }

    @Override
    public void unsubscribe(String reasonCodeId) {
        log.trace("Unsubscribe called");

        var loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var username = loggedInUser.getUsername();
        var password = loggedInUser.getPassword();

        var user = userService.getUser(username, password);
        var state = user.getState();

        if (user.getState().equals(AgentState.LOGOUT) || !xmppConnectionFactory.isConnected(user.getUsername())) {
            throw new AgentStateException("Agent is not connected. Please connect first");
        }

        log.trace("Agent state before logout {}", state);

            if (!signOutFromFinesse(reasonCodeId)) {
                log.trace("Finesse sign out failed");
                log.trace("Throwing FinesseApiRequestFailedException");
                throw new FinesseApiRequestFailedException("Finesse sign out failed");
            }
            checkIfUserLoggedOut(username, password);

        xmppConnectionFactory.disconnect(username);


    }

    private boolean signInToFinesse(int extension) {
        log.trace("Sign in to Finesse init");
        var statusCodeFromSignIn = userService.signIn(extension);
        log.trace("Status code from Finesse Sign in API is {}", statusCodeFromSignIn);
        return statusCodeFromSignIn == 202;
    }

    private AgentDto checkAndGetIfUserLoggedInAndExtensionIsMatching(String username, String password, int extension) {

        log.trace("Getting user after Sign in http request");

        var userAfterSignIn = userService.getUser(username, password);

        log.trace("User after sign in {}", userAfterSignIn);

        if ((userAfterSignIn == null || userAfterSignIn.getState().equals(AgentState.LOGOUT))) {
            log.trace("User still logged out");
            log.trace("Disconnection xmpp connection");
            xmppConnectionFactory.disconnect(username);
            log.trace("Throwing FinesseApiRequestFailedException");
            throw new FinesseApiRequestFailedException("Finesse sign in failed");
        }

        if (userAfterSignIn.getExtension() != 0 && extension != userAfterSignIn.getExtension()) {
            log.trace("Wrong extension entered");
            log.trace("Disconnection xmpp connection");
            xmppConnectionFactory.disconnect(username);
            log.trace("Throwing FinesseApiRequestFailedException");
            throw new FinesseApiRequestFailedException("Invalid extension number");
        }

        return new AgentDto(userAfterSignIn.getUsername(), userAfterSignIn.getState());

    }


    private boolean signOutFromFinesse(String reasonCodeID) {
        log.trace("Sign out from Finesse init");
        var statusCodeFromSignOut = userService.singOut(reasonCodeID);
        return statusCodeFromSignOut == 202;
    }


    private void checkIfUserLoggedOut(String username, String password) {
        log.trace("Getting user after Sign out http request");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.trace("Can not sleep thread");
            log.error(e.getMessage());
            log.catching(Level.ERROR, e.getCause());
            log.trace("Throwing FinesseApiRequestFailedException");
            throw new FinesseApiRequestFailedException("Finesse sign out failed");
        }

        var userAfterSignIn = userService.getUser(username, password);

        log.trace("User after sign out {}", userAfterSignIn);


        if ((userAfterSignIn == null || !userAfterSignIn.getState().equals(AgentState.LOGOUT))) {
            log.trace("Agent is not logged out");
            log.trace("Throwing FinesseApiRequestFailedException");
            throw new FinesseApiRequestFailedException("Finesse sign out failed");
        }
    }

}
