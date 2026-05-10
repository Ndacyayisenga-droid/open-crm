package com.openelements.crm.comment;

import com.openelements.spring.base.security.user.UserService;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.comment.CommentRepository;
import com.openelements.spring.base.services.comment.CommentService;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the standalone {@link CommentService} / {@link CommentRepository} /
 * {@link CommentEntity} from {@code spring-services} into the CRM application
 * context.
 *
 * <p>{@code spring-services} 0.13.0 does not ship a {@code CommentConfig}, so
 * the lib's {@code CommentService} carries no {@code @Service} stereotype. The
 * {@link AutoConfigurationPackage} annotation here registers the lib's comment
 * package as an additional auto-configuration package, which lets Spring Boot's
 * default JPA scanner pick up {@link CommentEntity} and the
 * {@link CommentRepository} interface alongside the existing CRM packages.</p>
 */
@Configuration
@AutoConfigurationPackage(basePackageClasses = CommentEntity.class)
public class CommentConfig {

    @Bean
    public CommentService commentService(final CommentRepository commentRepository,
                                         final UserService userService,
                                         final ApplicationEventPublisher applicationEventPublisher) {
        return new CommentService(commentRepository, userService, applicationEventPublisher);
    }
}
