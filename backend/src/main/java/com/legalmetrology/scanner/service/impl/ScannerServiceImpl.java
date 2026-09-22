package com.legalmetrology.scanner.service.impl;

import com.legalmetrology.common.enums.ScanType;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.product.dto.ProductResponse;
import com.legalmetrology.product.repository.ProductRepository;
import com.legalmetrology.product.service.ProductService;
import com.legalmetrology.scanner.dto.ScanRequest;
import com.legalmetrology.scanner.dto.ScanResponse;
import com.legalmetrology.scanner.entity.Scan;
import com.legalmetrology.scanner.mapper.ScanMapper;
import com.legalmetrology.scanner.repository.ScanRepository;
import com.legalmetrology.scanner.service.ScannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScannerServiceImpl implements ScannerService {

    private final ScanRepository scanRepository;
    private final InspectionRepository inspectionRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ScanMapper scanMapper;

    @Override
    @Transactional
    public ScanResponse recordScan(ScanRequest request) {
        Inspection inspection = inspectionRepository.findById(request.inspectionId())
                .orElseThrow(() -> ResourceNotFoundException.of("Inspection", request.inspectionId()));

        Scan scan = Scan.builder()
                .inspection(inspection)
                .scanType(request.scanType())
                .scanValue(request.scanValue())
                .build();
        scan = scanRepository.save(scan);

        ProductResponse matchedProduct = resolveMatchedProduct(request);

        /*
         * A matched barcode/QR scan is real, confirmed evidence of which
         * product this inspection is about — but until now the match was
         * only ever returned in this response and immediately discarded;
         * the inspection's own product_id was never updated. That's the
         * real reason a successfully scanned-and-matched inspection still
         * showed up everywhere (Reports, Dashboard, Copilot) as
         * "Untitled Inspection": InspectionResponse.productName is derived
         * from inspection.product, which stayed null forever. Only attach
         * on the first match (never overwrite a product the inspector may
         * have already confirmed some other way), and only for a real
         * match — an unmatched scan still gets recorded above so history
         * isn't lost, it just can't name the inspection.
         */
        if (matchedProduct != null && inspection.getProduct() == null) {
            productRepository.findById(matchedProduct.id()).ifPresent(product -> {
                inspection.setProduct(product);
                inspectionRepository.save(inspection);
            });
        }

        return scanMapper.toResponse(scan, matchedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanResponse> listByInspection(UUID inspectionId) {
        return scanRepository.findByInspectionId(inspectionId).stream()
                .map(scan -> scanMapper.toResponse(scan, resolveMatchedProduct(
                        new ScanRequest(inspectionId, scan.getScanType(), scan.getScanValue()))))
                .toList();
    }

    private ProductResponse resolveMatchedProduct(ScanRequest request) {
        if (request.scanType() == ScanType.BARCODE) {
            return productService.findByBarcode(request.scanValue()).orElse(null);
        }
        return null;
    }
}
