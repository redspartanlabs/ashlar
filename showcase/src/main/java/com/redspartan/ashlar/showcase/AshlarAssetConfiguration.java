package com.redspartan.ashlar.showcase;

import com.redspartan.ashlar.AshlarAssets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * The entire Spring-side integration Ashlar asks for: tell it what prefix its
 * own assets are served under.
 *
 * <p>Ashlar ships its scripts inside the jar at
 * {@code META-INF/resources/ashlar/js/}, which a servlet container exposes at
 * {@code /ashlar/js/...} relative to the application root. Under a context
 * path that becomes {@code /myapp/ashlar/js/...}, and only the application
 * knows its own context path - so it hands that over once, here.
 *
 * <p>This is a class in the consuming application on purpose. Ashlar could
 * have shipped a Spring Boot auto-configuration to do it automatically, but
 * that would make a component library depend on one application framework to
 * solve four lines of wiring. Revisit only if consumers actually ask.
 */
@Configuration
class AshlarAssetConfiguration {

    AshlarAssetConfiguration(@Value("${server.servlet.context-path:}") String contextPath) {
        AshlarAssets.basePath(contextPath + "/ashlar");
    }
}
