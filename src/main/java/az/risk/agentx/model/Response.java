package az.risk.agentx.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@SuperBuilder
@JsonInclude(NON_NULL)
public class Response<T> {
    protected LocalDateTime timeStamp;
    protected int status;
    protected HttpStatus error;
    protected String message;
    protected T data;
}
