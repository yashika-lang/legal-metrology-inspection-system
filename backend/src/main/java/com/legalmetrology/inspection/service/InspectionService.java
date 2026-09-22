package com.legalmetrology.inspection.service;

import com.legalmetrology.inspection.dto.InspectionRequest;
import com.legalmetrology.inspection.dto.InspectionResponse;
import com.legalmetrology.inspection.dto.InspectionStatusHistoryResponse;
import com.legalmetrology.inspection.dto.InspectionStatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InspectionService {

    InspectionResponse create(InspectionRequest request, UUID inspectorId);

    InspectionResponse getById(UUID id);

    /**
     * @param evaluatedOnly When true, only inspections the AI Pipeline has actually
     *                      evaluated (compliance score present) are returned — used
     *                      by Reports and the Dashboard so a draft that was created
     *                      but never worked on doesn't clutter either list.
     */
    Page<InspectionResponse> list(UUID inspectorId, boolean evaluatedOnly, Pageable pageable);

    InspectionResponse updateStatus(UUID id, InspectionStatusUpdateRequest request, UUID changedByUserId);

    List<InspectionStatusHistoryResponse> getStatusHistory(UUID id);
}
