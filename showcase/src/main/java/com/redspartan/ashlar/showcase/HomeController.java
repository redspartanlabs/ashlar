package com.redspartan.ashlar.showcase;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Passes the context path through for the application's OWN stylesheet
     * link. Ashlar's assets do not need this - they resolve through
     * {@code AshlarAssets} inside the component templates themselves - but the
     * showcase still has to solve the problem for its own files, and doing it
     * here keeps the two concerns visibly separate.
     */
    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        model.addAttribute("contextPath", request.getContextPath());
        return "pages/home";
    }
}
