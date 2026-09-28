package com.redspartanlabs.ashlar.showcase;

import com.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class HomeController {

    /**
     * Passes the context path through for the application's OWN stylesheet and
     * navigation links. Ashlar's assets do not need this - they resolve through
     * {@code AshlarAssets} inside the component templates themselves - but the
     * showcase still has to solve the problem for its own files, and doing it
     * here keeps the two concerns visibly separate.
     */
    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        model.addAttribute("contextPath", request.getContextPath());
        return "pages/home";
    }

    /**
     * One route for every component showcase page rather than a method each -
     * {@link ComponentCatalog} is the single source of truth for which slugs
     * are valid, so a slug with no catalog entry is a 404 rather than a
     * template-not-found error.
     */
    @GetMapping("/components/{slug}")
    public String componentPage(@PathVariable String slug, HttpServletRequest request, Model model) {
        try {
            ComponentCatalog.bySlug(slug);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No component page for '" + slug + "'");
        }
        model.addAttribute("contextPath", request.getContextPath());
        return "pages/components/" + slug;
    }
}
