package com.legalmetrology.scanner.service;

import com.legalmetrology.scanner.dto.ScanRequest;
import com.legalmetrology.scanner.dto.ScanResponse;

import java.util.List;
import java.util.UUID;

public interface ScannerService {

    ScanResponse recordScan(ScanRequest request);

    List<ScanResponse> listByInspection(UUID inspectionId);
}
