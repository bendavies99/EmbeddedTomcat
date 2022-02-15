package net.bdavies;

import lombok.val;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author ben.davies
 */
@WebServlet(
        name = "HelloServlet",
        urlPatterns = {"/hello"}
)
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        val out = resp.getOutputStream();
        out.write("Servlet works test".getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

}
