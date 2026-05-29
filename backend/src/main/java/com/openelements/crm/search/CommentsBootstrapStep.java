package com.openelements.crm.search;

import com.openelements.crm.search.SearchIndexService.OwnerRef;
import com.openelements.spring.base.services.search.SearchIndexBootstrapStep;
import com.openelements.spring.base.services.comment.CommentDto;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.comment.CommentRepository;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bootstrap step that streams all comments into the comments index. Comments
 * are owner-resolved across the three join tables; orphan comments (not yet
 * attached to any owner) are skipped.
 */
@Component
@Order(40)
public class CommentsBootstrapStep implements SearchIndexBootstrapStep {

    private final CrmIndexNames indexNames;
    private final CommentRepository commentRepository;
    private final SearchIndexService indexService;

    public CommentsBootstrapStep(final CrmIndexNames indexNames,
                                 final CommentRepository commentRepository,
                                 final SearchIndexService indexService) {
        this.indexNames = indexNames;
        this.commentRepository = commentRepository;
        this.indexService = indexService;
    }

    @Override
    public String indexUid() {
        return indexNames.comments();
    }

    @Override
    public Stream<Map<String, Object>> documents() {
        return commentRepository.findAll().stream()
            .map(this::toDocumentOrNull)
            .filter(Objects::nonNull);
    }

    private Map<String, Object> toDocumentOrNull(final CommentEntity entity) {
        final Optional<OwnerRef> owner = indexService.resolveCommentOwner(entity.getId());
        if (owner.isEmpty()) {
            return null;
        }
        final CommentDto dto = new CommentDto(
            entity.getId(), entity.getText(), null, entity.getCreatedAt(), entity.getUpdatedAt());
        return indexService.commentDocument(dto, owner.get());
    }
}
