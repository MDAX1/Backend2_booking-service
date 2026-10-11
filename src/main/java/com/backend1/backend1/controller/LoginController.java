package com.backend1.backend1.controller;

import com.backend1.backend1.client.AuthClient;
import com.backend1.backend1.config.SessionBearerTokenResolver;
import com.backend1.backend1.exception.CustomerServiceUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;

@Controller
public class LoginController {

    private final AuthClient authClient;
    private final String customerServiceUrl;

    public LoginController(AuthClient authClient,
                           @Value("${customer.service.url:http://localhost:8080}") String customerServiceUrl) {
        this.authClient = authClient;
        this.customerServiceUrl = customerServiceUrl;
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {
        try {
            String token = authClient.login(username, password);
            var session = request.getSession(true);
            session.setAttribute(SessionBearerTokenResolver.SESSION_TOKEN_KEY, token);
            session.setAttribute("LOGGED_IN_USER", username);
            return "redirect:/bookings";
        } catch (HttpClientErrorException.Unauthorized e) {
            model.addAttribute("errorMessage", "Fel användarnamn eller lösenord");
            return "login";
        } catch (CustomerServiceUnavailableException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logoutGet(HttpServletRequest request) {
        return logout(request);
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    @GetMapping("/customers")
    public String redirectToCustomerService(HttpServletRequest request) {
        var session = request.getSession(false);
        String token = session != null ? (String) session.getAttribute(SessionBearerTokenResolver.SESSION_TOKEN_KEY) : null;
        if (token != null && !token.isBlank()) {
            return "redirect:" + customerServiceUrl + "/customers?token=" + token;
        }
        return "redirect:" + customerServiceUrl + "/customers";
    }
}
