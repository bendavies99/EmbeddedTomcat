package net.bdavies.tomcat.server.exceptions;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ben.davies
 */
@Slf4j
public class ArgumentNotPresentException extends RuntimeException {
    public ArgumentNotPresentException(String arg) {
        super(arg + " is not present at runtime please set it");
    }
}
