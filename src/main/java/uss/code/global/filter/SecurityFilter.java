package uss.code.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;

@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if(!requestURI.equals("/api/v1/queue/sub")){
            setErrorResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void setErrorResponse(
            HttpServletResponse response
    )throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(SC_FORBIDDEN);
    }
}
