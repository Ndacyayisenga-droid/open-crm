package com.openelements.crm.search;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.spring.base.services.search.SearchIndexBootstrapStep;
import com.openelements.spring.base.services.tag.TagEntity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bootstrap step that streams all companies into the companies index. Maps
 * each {@link CompanyEntity} to a {@link CompanyDto} and then to a Meilisearch
 * document via {@link SearchIndexService}.
 */
@Component
@Order(10)
public class CompaniesBootstrapStep implements SearchIndexBootstrapStep {

    private final CrmIndexNames indexNames;
    private final CompanyRepository companyRepository;
    private final SearchIndexService indexService;

    public CompaniesBootstrapStep(final CrmIndexNames indexNames,
                                  final CompanyRepository companyRepository,
                                  final SearchIndexService indexService) {
        this.indexNames = indexNames;
        this.companyRepository = companyRepository;
        this.indexService = indexService;
    }

    @Override
    public String indexUid() {
        return indexNames.companies();
    }

    @Override
    public Stream<Map<String, Object>> documents() {
        return companyRepository.findAll().stream()
            .map(CompaniesBootstrapStep::mapCompany)
            .map(indexService::companyDocument);
    }

    private static CompanyDto mapCompany(final CompanyEntity entity) {
        Objects.requireNonNull(entity);
        return new CompanyDto(
            entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getWebsite(),
            entity.getStreet(),
            entity.getHouseNumber(),
            entity.getZipCode(),
            entity.getCity(),
            entity.getCountry(),
            entity.getPhoneNumber(),
            entity.getDescription(),
            entity.getBankName(),
            entity.getBic(),
            entity.getIban(),
            entity.getVatId(),
            entity.getLogo() != null,
            entity.getBrevoCompanyId() != null,
            0L,
            0L,
            entity.getTags() == null ? List.of()
                : entity.getTags().stream().map(TagEntity::getId).toList(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }
}
