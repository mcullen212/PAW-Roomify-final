package ar.edu.itba.paw.interfaces.service;

import java.util.Locale;
import java.util.Map;

public interface MailContentBuilder {
    String build(String templateName, Map<String, Object> variables, Locale locale);
}
