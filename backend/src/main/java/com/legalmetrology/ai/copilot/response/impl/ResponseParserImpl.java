package com.legalmetrology.ai.copilot.response.impl;

import com.legalmetrology.ai.copilot.response.ResponseParser;
import com.legalmetrology.ai.llm.provider.LlmResponseUtil;
import org.springframework.stereotype.Component;

@Component
public class ResponseParserImpl implements ResponseParser {

    @Override
    public String parse(String rawLlmResponse) {
        if (rawLlmResponse == null) {
            return "";
        }
        return LlmResponseUtil.stripMarkdownFences(rawLlmResponse);
    }
}
