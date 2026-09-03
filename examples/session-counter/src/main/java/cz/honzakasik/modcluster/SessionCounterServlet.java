package cz.honzakasik.modcluster;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/")
public final class SessionCounterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String COUNTER_ATTRIBUTE = SessionCounterServlet.class.getName() + ".counter";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestedSessionId = request.getRequestedSessionId();
        boolean resumedSession = requestedSessionId != null && request.isRequestedSessionIdValid();

        HttpSession session = request.getSession();
        Integer previousValue = (Integer) session.getAttribute(COUNTER_ATTRIBUTE);
        int counter = previousValue == null ? 1 : previousValue + 1;

        // Setting the attribute explicitly makes each counter change visible
        // to whichever session manager the selected build uses.
        session.setAttribute(COUNTER_ATTRIBUTE, counter);

        String nodeName = System.getProperty("jboss.node.name", "unknown");

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-WildFly-Node", nodeName);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().printf(
                "node=%s%ncount=%d%nsession=%s%nresumed=%s%n",
                nodeName,
                counter,
                session.getId(),
                resumedSession);
    }
}
