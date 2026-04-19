package com.ahs.cvm.application.parameter;

import com.ahs.cvm.domain.enums.SystemParameterType;
import com.ahs.cvm.persistence.parameter.SystemParameter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class SystemParameterValidator {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern HOST = Pattern.compile("^[A-Za-z0-9.-]+$");
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

    public void validate(SystemParameter parameter, String value) {
        if (value == null || value.isBlank()) {
            if (parameter.isRequired()) {
                throw new IllegalArgumentException("Wert ist erforderlich");
            }
            return;
        }
        validateType(parameter.getType(), value);
        validateRules(parameter.getValidationRules(), value);
        validateOptions(parameter.getType(), parameter.getOptions(), value);
    }

    private void validateType(SystemParameterType type, String value) {
        switch (type) {
            case INTEGER -> {
                try {
                    Long.parseLong(value.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Wert muss eine Ganzzahl sein");
                }
            }
            case DECIMAL -> {
                try {
                    new BigDecimal(value.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Wert muss eine Dezimalzahl sein");
                }
            }
            case BOOLEAN -> {
                String v = value.trim().toLowerCase();
                if (!v.equals("true") && !v.equals("false")) {
                    throw new IllegalArgumentException("Wert muss true oder false sein");
                }
            }
            case EMAIL -> {
                if (!EMAIL.matcher(value.trim()).matches()) {
                    throw new IllegalArgumentException("Wert ist keine gültige E-Mail-Adresse");
                }
            }
            case URL -> {
                try {
                    URI.create(value.trim()).toURL();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Wert ist keine gültige URL");
                }
            }
            case HOST -> {
                if (!HOST.matcher(value.trim()).matches()) {
                    throw new IllegalArgumentException("Wert ist kein gültiger Hostname");
                }
            }
            case IP -> {
                String v = value.trim();
                if (!IPV4.matcher(v).matches()) {
                    throw new IllegalArgumentException("Wert ist keine gültige IPv4-Adresse");
                }
                for (String part : v.split("\\.")) {
                    int n = Integer.parseInt(part);
                    if (n < 0 || n > 255) {
                        throw new IllegalArgumentException("IP-Oktett ausserhalb 0-255");
                    }
                }
            }
            case DATE -> {
                try {
                    LocalDate.parse(value.trim());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Wert ist kein gültiges Datum (YYYY-MM-DD)");
                }
            }
            case TIMESTAMP -> {
                try {
                    LocalDateTime.parse(value.trim());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Wert ist kein gültiger Zeitstempel");
                }
            }
            case JSON -> {
                String v = value.trim();
                if (!(v.startsWith("{") || v.startsWith("["))) {
                    throw new IllegalArgumentException("Wert ist kein gültiges JSON");
                }
            }
            default -> {
                // STRING, PASSWORD, SELECT, MULTISELECT, TEXTAREA -> keine Typprüfung
            }
        }
    }

    private void validateRules(String rules, String value) {
        if (rules == null || rules.isBlank()) {
            return;
        }
        try {
            if (!Pattern.compile(rules).matcher(value).matches()) {
                throw new IllegalArgumentException("Wert entspricht nicht dem erwarteten Muster");
            }
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Validierungsmuster ist ungültig: " + e.getMessage());
        }
    }

    private void validateOptions(SystemParameterType type, String options, String value) {
        if (options == null || options.isBlank()) {
            return;
        }
        List<String> allowed = Arrays.stream(options.split(",")).map(String::trim).toList();
        if (type == SystemParameterType.SELECT) {
            if (!allowed.contains(value.trim())) {
                throw new IllegalArgumentException("Wert ist keine zulässige Option");
            }
        } else if (type == SystemParameterType.MULTISELECT) {
            for (String part : value.split(",")) {
                if (!allowed.contains(part.trim())) {
                    throw new IllegalArgumentException("Auswahl '" + part.trim() + "' ist nicht zulässig");
                }
            }
        }
    }
}
