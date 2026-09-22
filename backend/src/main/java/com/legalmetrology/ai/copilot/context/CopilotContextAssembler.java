package com.legalmetrology.ai.copilot.context;

import java.util.UUID;

/** Builds a {@link CopilotContext} for one inspection — the single place in this module that reads other modules' repositories. */
public interface CopilotContextAssembler {

    CopilotContext assemble(UUID inspectionId);
}
