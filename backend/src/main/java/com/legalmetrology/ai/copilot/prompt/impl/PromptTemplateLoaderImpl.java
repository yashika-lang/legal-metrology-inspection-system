package com.legalmetrology.ai.copilot.prompt.impl;

import com.legalmetrology.ai.copilot.prompt.PromptTemplateLoader;
import com.legalmetrology.exception.BadRequestException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PromptTemplateLoaderImpl implements PromptTemplateLoader {

    private static final String TEMPLATE_PATH_FORMAT = "prompts/copilot/%s.txt";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Override
    public String render(String templateName, Map<String, String> variables) {
        String template = cache.computeIfAbsent(templateName, this::loadFromClasspath);

        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }

    private String loadFromClasspath(String templateName) {
        String path = TEMPLATE_PATH_FORMAT.formatted(templateName);
        try (var inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BadRequestException("Prompt template not found: " + path);
        }
    }
}
