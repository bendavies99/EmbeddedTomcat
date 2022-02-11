package net.bdavies;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Objects;

/**
 * @author ben.davies
 */
@Slf4j
public class ExampleServlet extends HttpServlet {
    @SneakyThrows
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        renderBanner();
        InitialContext context = new InitialContext();
        String value = context.lookup("java:/comp/env/testEnv").toString();
        log.info("Does Context work?: {}", value);
    }

    @SneakyThrows
    private void renderBanner() {
        val is = Thread.currentThread().getContextClassLoader().getResourceAsStream("banner.txt");
        val br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("Requesting from:: " + req.getRequestURI());
        if (req.getRequestURI().equals("/test/index")) {
            resp.setContentType("text/html");
            val url = Thread.currentThread().getContextClassLoader().getResource("index.html");
            assert url != null;
            log.info("The url path: {}", url.getPath());
            val is = new File(url.getPath());
            val br = new BufferedReader(new FileReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                resp.getWriter().println(line);
            }
            resp.getWriter().flush();
        } else {
            super.doGet(req, resp);
        }
    }
}
