package com.legalmetrology.inspection.service.impl;

import com.legalmetrology.auth.entity.User;
import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.InspectionStatus;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.dto.InspectionRequest;
import com.legalmetrology.inspection.dto.InspectionResponse;
import com.legalmetrology.inspection.dto.InspectionStatusHistoryResponse;
import com.legalmetrology.inspection.dto.InspectionStatusUpdateRequest;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.entity.InspectionStatusHistory;
import com.legalmetrology.inspection.mapper.InspectionMapper;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.inspection.repository.InspectionStatusHistoryRepository;
import com.legalmetrology.inspection.service.InspectionService;
import com.legalmetrology.evidence.service.EvidenceService;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import com.legalmetrology.ocr.repository.FusedDeclarationRepository;
import com.legalmetrology.product.entity.Product;
import com.legalmetrology.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionServiceImpl implements InspectionService {

    private static final Map<InspectionStatus, Set<InspectionStatus>> ALLOWED_TRANSITIONS = Map.of(
            InspectionStatus.DRAFT, EnumSet.of(InspectionStatus.IN_PROGRESS),
            InspectionStatus.IN_PROGRESS, EnumSet.of(InspectionStatus.PENDING_REVIEW),
            InspectionStatus.PENDING_REVIEW, EnumSet.of(InspectionStatus.COMPLETED, InspectionStatus.IN_PROGRESS),
            InspectionStatus.COMPLETED, EnumSet.of(InspectionStatus.CLOSED),
            InspectionStatus.CLOSED, EnumSet.noneOf(InspectionStatus.class)
    );

    private final InspectionRepository inspectionRepository;
    private final InspectionStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InspectionMapper inspectionMapper;
    private final EvidenceService evidenceService;
    private final FusedDeclarationRepository fusedDeclarationRepository;

    @Override
    @Transactional
    public InspectionResponse create(InspectionRequest request, UUID inspectorId) {
        User inspector = userRepository.findById(inspectorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", inspectorId));

        Product product = null;
        if (request.productId() != null) {
            product = productRepository.findById(request.productId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", request.productId()));
        }

        Inspection inspection = Inspection.builder()
                .inspector(inspector)
                .product(product)
                .locationLat(request.locationLat())
                .locationLng(request.locationLng())
                .region(request.region())
                .status(InspectionStatus.DRAFT)
                .startedAt(Instant.now())
                .build();

        inspection = inspectionRepository.save(inspection);
        recordStatusHistory(inspection, null, InspectionStatus.DRAFT, inspector, "Inspection created");

        return inspectionMapper.toResponse(inspection);
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionResponse getById(UUID id) {
        Inspection inspection = findInspectionOrThrow(id);
        String fallbackProductName = inspection.getProduct() == null
                ? fusedDeclarationRepository.findByInspectionIdAndDeclarationType(id, DeclarationType.PRODUCT_NAME)
                        .filter(FusedDeclaration::isPresent)
                        .map(FusedDeclaration::getFusedValue)
                        .orElse(null)
                : null;
        return inspectionMapper.toResponse(inspection, fallbackProductName);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InspectionResponse> list(UUID inspectorId, boolean evaluatedOnly, Pageable pageable) {
        Page<Inspection> page = resolveListPage(inspectorId, evaluatedOnly, pageable);

        List<UUID> unnamedIds = page.getContent().stream()
                .filter(i -> i.getProduct() == null)
                .map(Inspection::getId)
                .toList();
        /*
         * A PRODUCT_NAME row exists for every inspection the fusion step has
         * ever run on, whether or not a name was actually found on the
         * label — `present=false` rows carry a null `fusedValue`.
         * `Collectors.toMap` throws NullPointerException on a null value
         * (it can't distinguish "absent" from "not yet computed" in a
         * HashMap), which took down this entire endpoint — every caller of
         * `GET /inspections`, including Reports and the Dashboard — the
         * moment any single inspection in the page had one of these
         * null-value rows. Only a genuinely *present* detection with a real
         * value is a usable fallback name.
         */
        Map<UUID, String> fallbackNames = unnamedIds.isEmpty()
                ? Map.of()
                : fusedDeclarationRepository.findByInspectionIdInAndDeclarationType(unnamedIds, DeclarationType.PRODUCT_NAME).stream()
                        .filter(fd -> fd.isPresent() && fd.getFusedValue() != null)
                        .collect(Collectors.toMap(fd -> fd.getInspection().getId(), FusedDeclaration::getFusedValue, (a, b) -> a));

        return page.map(inspection -> inspectionMapper.toResponse(inspection, fallbackNames.get(inspection.getId())));
    }

    private Page<Inspection> resolveListPage(UUID inspectorId, boolean evaluatedOnly, Pageable pageable) {
        if (inspectorId != null) {
            return evaluatedOnly
                    ? inspectionRepository.findByInspectorIdAndComplianceScoreIsNotNull(inspectorId, pageable)
                    : inspectionRepository.findByInspectorId(inspectorId, pageable);
        }
        return evaluatedOnly
                ? inspectionRepository.findByComplianceScoreIsNotNull(pageable)
                : inspectionRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public InspectionResponse updateStatus(UUID id, InspectionStatusUpdateRequest request, UUID changedByUserId) {
        Inspection inspection = findInspectionOrThrow(id);
        InspectionStatus currentStatus = inspection.getStatus();
        InspectionStatus targetStatus = request.status();

        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new BadRequestException(
                    "Cannot transition inspection from " + currentStatus + " to " + targetStatus);
        }

        User changedBy = userRepository.findById(changedByUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", changedByUserId));

        inspection.setStatus(targetStatus);
        if (targetStatus == InspectionStatus.COMPLETED) {
            inspection.setCompletedAt(Instant.now());
        }
        inspection = inspectionRepository.save(inspection);

        recordStatusHistory(inspection, currentStatus, targetStatus, changedBy, request.note());

        // Evidence becomes legally immutable the moment an inspection is finalized — see
        // Evidence's Javadoc. Generating evidence itself happens earlier in the pipeline
        // (rule evaluation -> evidence generation, both before COMPLETED); this only locks
        // whatever evidence already exists at that point.
        if (targetStatus == InspectionStatus.COMPLETED) {
            evidenceService.finalizeEvidence(id);
        }

        return inspectionMapper.toResponse(inspection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspectionStatusHistoryResponse> getStatusHistory(UUID id) {
        findInspectionOrThrow(id);
        return statusHistoryRepository.findByInspectionIdOrderByCreatedAtAsc(id).stream()
                .map(inspectionMapper::toResponse)
                .toList();
    }

    private void recordStatusHistory(Inspection inspection, InspectionStatus fromStatus,
                                      InspectionStatus toStatus, User changedBy, String note) {
        InspectionStatusHistory history = InspectionStatusHistory.builder()
                .inspection(inspection)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(changedBy)
                .note(note)
                .build();
        statusHistoryRepository.save(history);
    }

    private Inspection findInspectionOrThrow(UUID id) {
        return inspectionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", id));
    }
}
