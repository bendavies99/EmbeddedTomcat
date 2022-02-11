package net.bdavies;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import java.io.IOException;

/**
 * @author ben.davies
 */
@Slf4j
public class ResourceFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.info("Filter works");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
