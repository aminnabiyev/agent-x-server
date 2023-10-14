package az.risk.agentx.config.auth;

import az.risk.agentx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Log4j2
@Component
@RequiredArgsConstructor
public class CustomAuthProvider implements AuthenticationProvider {
    private final UserService userService;
    @Override
    public Authentication authenticate(Authentication authentication) {

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        var authorities = authentication.getAuthorities();

        log.trace("Authentication init for {}", username);



        var user = userService.getUser(username, password);



        if (user == null || !password.equals(user.getPassword())) {
            log.error("Invalid password");
            throw new BadCredentialsException("Invalid password");
        }

        log.info("Logged in {}", user);

        return new UsernamePasswordAuthenticationToken(user, password, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

}
