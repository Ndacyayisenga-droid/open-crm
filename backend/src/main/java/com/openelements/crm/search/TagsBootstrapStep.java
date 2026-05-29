package com.openelements.crm.search;

import com.openelements.spring.base.services.search.SearchIndexBootstrapStep;
import com.openelements.spring.base.services.tag.TagDto;
import com.openelements.spring.base.services.tag.TagEntity;
import com.openelements.spring.base.services.tag.TagRepository;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bootstrap step that streams all tags into the tags index.
 */
@Component
@Order(30)
public class TagsBootstrapStep implements SearchIndexBootstrapStep {

    private final CrmIndexNames indexNames;
    private final TagRepository tagRepository;
    private final SearchIndexService indexService;

    public TagsBootstrapStep(final CrmIndexNames indexNames,
                             final TagRepository tagRepository,
                             final SearchIndexService indexService) {
        this.indexNames = indexNames;
        this.tagRepository = tagRepository;
        this.indexService = indexService;
    }

    @Override
    public String indexUid() {
        return indexNames.tags();
    }

    @Override
    public Stream<Map<String, Object>> documents() {
        return tagRepository.findAll().stream()
            .map(TagsBootstrapStep::mapTag)
            .map(indexService::tagDocument);
    }

    private static TagDto mapTag(final TagEntity entity) {
        Objects.requireNonNull(entity);
        return new TagDto(entity.getId(), entity.getName(), entity.getDescription(), entity.getColor());
    }
}
