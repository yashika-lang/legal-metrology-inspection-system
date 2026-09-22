package com.legalmetrology.product.service.impl;

import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.exception.DuplicateResourceException;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.product.dto.ManufacturerResponse;
import com.legalmetrology.product.dto.ProductCategoryResponse;
import com.legalmetrology.product.dto.ProductRequest;
import com.legalmetrology.product.dto.ProductResponse;
import com.legalmetrology.product.entity.Manufacturer;
import com.legalmetrology.product.entity.Product;
import com.legalmetrology.product.entity.ProductCategory;
import com.legalmetrology.product.mapper.ProductMapper;
import com.legalmetrology.product.repository.ManufacturerRepository;
import com.legalmetrology.product.repository.ProductCategoryRepository;
import com.legalmetrology.product.repository.ProductRepository;
import com.legalmetrology.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request, UUID createdByUserId) {
        if (StringUtils.hasText(request.barcode()) && productRepository.existsByBarcode(request.barcode())) {
            throw new DuplicateResourceException("A product with this barcode already exists");
        }

        Product product = Product.builder()
                .name(request.name())
                .barcode(request.barcode())
                .defaultUnit(request.defaultUnit())
                .category(resolveCategory(request.categoryId()))
                .manufacturer(resolveManufacturer(request.manufacturerId()))
                .createdBy(createdByUserId != null ? userRepository.findById(createdByUserId).orElse(null) : null)
                .build();

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return productMapper.toResponse(findProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", barcode));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<ProductResponse> findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String nameQuery, Pageable pageable) {
        Page<Product> page = StringUtils.hasText(nameQuery)
                ? productRepository.findByNameContainingIgnoreCase(nameQuery, pageable)
                : productRepository.findAll(pageable);
        return page.map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findProductOrThrow(id);

        if (StringUtils.hasText(request.barcode())
                && !request.barcode().equals(product.getBarcode())
                && productRepository.existsByBarcode(request.barcode())) {
            throw new DuplicateResourceException("A product with this barcode already exists");
        }

        product.setName(request.name());
        product.setBarcode(request.barcode());
        product.setDefaultUnit(request.defaultUnit());
        product.setCategory(resolveCategory(request.categoryId()));
        product.setManufacturer(resolveManufacturer(request.manufacturerId()));

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> listCategories() {
        return categoryRepository.findAll().stream().map(productMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ProductCategoryResponse createCategory(String name, UUID parentCategoryId) {
        ProductCategory category = ProductCategory.builder()
                .name(name)
                .parentCategory(resolveCategory(parentCategoryId))
                .build();
        return productMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManufacturerResponse> listManufacturers() {
        return manufacturerRepository.findAll().stream().map(productMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ManufacturerResponse createManufacturer(String name, String gstin, String address, String region) {
        Manufacturer manufacturer = Manufacturer.builder()
                .name(name)
                .gstin(gstin)
                .address(address)
                .region(region)
                .build();
        return productMapper.toResponse(manufacturerRepository.save(manufacturer));
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    private ProductCategory resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductCategory", categoryId));
    }

    private Manufacturer resolveManufacturer(UUID manufacturerId) {
        if (manufacturerId == null) {
            return null;
        }
        return manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Manufacturer", manufacturerId));
    }
}
