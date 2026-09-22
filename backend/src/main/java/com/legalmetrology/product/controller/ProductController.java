package com.legalmetrology.product.controller;

import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.common.response.PagedResponse;
import com.legalmetrology.product.dto.CreateCategoryRequest;
import com.legalmetrology.product.dto.CreateManufacturerRequest;
import com.legalmetrology.product.dto.ManufacturerResponse;
import com.legalmetrology.product.dto.ProductCategoryResponse;
import com.legalmetrology.product.dto.ProductRequest;
import com.legalmetrology.product.dto.ProductResponse;
import com.legalmetrology.product.service.ProductService;
import com.legalmetrology.security.SecurityUtils;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Products", description = "Product master data — the goods inspections are performed against")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Register a new product",
            description = "Creates a new product master-data record. `barcode` is optional but, if provided, must be unique — "
                    + "attempting to register a second product with the same barcode fails with 409. Any authenticated user may create a product.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Resource created successfully","data":{"id":"3f2a9c10-6b1e-4a2e-9c1a-1e2f3a4b5c6d","name":"Aashirvaad Atta 5kg","categoryId":"1a2b3c4d-5e6f-4789-9abc-def012345678","categoryName":"Food & Grocery","manufacturerId":"9d8c7b6a-5f4e-4321-8765-abcdef012345","manufacturerName":"ITC Limited","barcode":"8901058854488","defaultUnit":"kg","createdAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — e.g. `name` blank, `name` over 255 characters, or `barcode` fails the checksum/format check"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "A product with this barcode already exists")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseUtil.created(productService.create(request, securityUtils.getCurrentUserId()));
    }

    @Operation(summary = "Search products by name (paginated)",
            description = "Case-insensitive partial match on product name. Omit `query` to list all products page by page.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of matching products", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":{"content":[{"id":"3f2a9c10-6b1e-4a2e-9c1a-1e2f3a4b5c6d","name":"Aashirvaad Atta 5kg","categoryId":"1a2b3c4d-5e6f-4789-9abc-def012345678","categoryName":"Food & Grocery","manufacturerId":"9d8c7b6a-5f4e-4321-8765-abcdef012345","manufacturerName":"ITC Limited","barcode":"8901058854488","defaultUnit":"kg","createdAt":"2026-01-15T10:30:00Z"}],"page":0,"size":20,"totalElements":1,"totalPages":1,"last":true},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> search(
            @Parameter(description = "Partial, case-insensitive product name to search for", example = "Atta")
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseUtil.ok(PagedResponse.of(productService.search(query, pageable)));
    }

    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":{"id":"3f2a9c10-6b1e-4a2e-9c1a-1e2f3a4b5c6d","name":"Aashirvaad Atta 5kg","categoryId":"1a2b3c4d-5e6f-4789-9abc-def012345678","categoryName":"Food & Grocery","manufacturerId":"9d8c7b6a-5f4e-4321-8765-abcdef012345","manufacturerName":"ITC Limited","barcode":"8901058854488","defaultUnit":"kg","createdAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No product exists with the given id", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":false,"message":"Product not found with id: 3f2a9c10-6b1e-4a2e-9c1a-1e2f3a4b5c6d","timestamp":"2026-01-15T10:30:00Z"}
                    """)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable UUID id) {
        return ResponseUtil.ok(productService.getById(id));
    }

    @Operation(summary = "Look up a product by its barcode")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":{"id":"3f2a9c10-6b1e-4a2e-9c1a-1e2f3a4b5c6d","name":"Aashirvaad Atta 5kg","categoryId":"1a2b3c4d-5e6f-4789-9abc-def012345678","categoryName":"Food & Grocery","manufacturerId":"9d8c7b6a-5f4e-4321-8765-abcdef012345","manufacturerName":"ITC Limited","barcode":"8901058854488","defaultUnit":"kg","createdAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No product exists with the given barcode")
    })
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<ProductResponse>> getByBarcode(
            @Parameter(description = "Exact product barcode (EAN-13/UPC)", example = "8901058854488")
            @PathVariable String barcode) {
        return ResponseUtil.ok(productService.getByBarcode(barcode));
    }

    @Operation(summary = "Update an existing product",
            description = "Full replace of the product's editable fields. Changing `barcode` to one already used by another product fails with 409.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Product updated successfully","data":{"id":"3f2a9c10-6b1e-4a2e-9c1a-1e2f3a4b5c6d","name":"Aashirvaad Atta 10kg","categoryId":"1a2b3c4d-5e6f-4789-9abc-def012345678","categoryName":"Food & Grocery","manufacturerId":"9d8c7b6a-5f4e-4321-8765-abcdef012345","manufacturerName":"ITC Limited","barcode":"8901058854488","defaultUnit":"kg","createdAt":"2026-01-15T10:30:00Z"},"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed on the request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No product exists with the given id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Another product already uses this barcode")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ResponseUtil.ok("Product updated successfully", productService.update(id, request));
    }

    @Operation(summary = "Delete a product (admin/senior officer only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Product deleted successfully","timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller is not ADMIN or SENIOR_OFFICER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No product exists with the given id")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SENIOR_OFFICER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseUtil.noContent("Product deleted successfully");
    }

    @Operation(summary = "List all product categories")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All categories", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"1a2b3c4d-5e6f-4789-9abc-def012345678","name":"Food & Grocery","parentCategoryId":null}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ProductCategoryResponse>>> listCategories() {
        return ResponseUtil.ok(productService.listCategories());
    }

    @Operation(summary = "Create a product category",
            description = "Master data used to classify products — there is no seed data for this, so the first user " +
                    "to need a category creates it. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is blank"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<ProductCategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseUtil.created(productService.createCategory(request.name(), request.parentCategoryId()));
    }

    @Operation(summary = "List all manufacturers")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All manufacturers", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {"success":true,"message":"Request completed successfully","data":[{"id":"9d8c7b6a-5f4e-4321-8765-abcdef012345","name":"ITC Limited","gstin":"29AABCT1332L1ZU","address":"37 J.C. Road, Bengaluru, Karnataka","region":"South"}],"timestamp":"2026-01-15T10:30:00Z"}
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/manufacturers")
    public ResponseEntity<ApiResponse<List<ManufacturerResponse>>> listManufacturers() {
        return ResponseUtil.ok(productService.listManufacturers());
    }

    @Operation(summary = "Create a manufacturer",
            description = "Master data identifying who makes/packs/imports a product — there is no seed data for " +
                    "this, so the first user to need one creates it. Authorization: any authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Manufacturer created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is blank"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping("/manufacturers")
    public ResponseEntity<ApiResponse<ManufacturerResponse>> createManufacturer(@Valid @RequestBody CreateManufacturerRequest request) {
        return ResponseUtil.created(productService.createManufacturer(request.name(), request.gstin(), request.address(), request.region()));
    }
}
