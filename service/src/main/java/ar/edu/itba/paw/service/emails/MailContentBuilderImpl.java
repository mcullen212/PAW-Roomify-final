package ar.edu.itba.paw.service.emails;

import ar.edu.itba.paw.interfaces.service.MailContentBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Service
public class MailContentBuilderImpl implements MailContentBuilder {

    private final TemplateEngine templateEngine;

    public MailContentBuilderImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String build(String templateName, Map<String, Object> variables, Locale locale) {
        // Renderiza solo el template que le pasamos, sin envolver en baseEmail
        Context context = new Context(locale);
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }
}
