package com.openelements.crm.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.apikey.ApiKeyController;
import com.openelements.crm.auditlog.AuditLogController;
import com.openelements.crm.brevo.BrevoSyncController;
import com.openelements.crm.company.CompanyController;
import com.openelements.crm.contact.ContactController;
import com.openelements.crm.tag.TagController;
import com.openelements.crm.task.TaskController;
import com.openelements.crm.user.UserController;
import com.openelements.crm.webhook.WebhookController;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * Verifies that every delete endpoint carries {@code @RequiresAdmin}
 * and every admin controller carries a class-level
 * {@code @RequiresItAdmin}. This test pins the security contract
 * defined in spec 085 without requiring a full Spring context.
 *
 * <p>Complements the behavioral integration tests in {@link SecurityRoleIntegrationTest}
 * which verify the runtime 403/204 responses.
 */
class PreAuthorizeAnnotationTest {

    @Test
    void companyDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(CompanyController.class.getDeclaredMethod(
            "delete", UUID.class, boolean.class));
    }

    @Test
    void companyDeleteCommentRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(CompanyController.class.getDeclaredMethod(
            "deleteComment", UUID.class, UUID.class));
    }

    @Test
    void contactDeleteCommentRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(ContactController.class.getDeclaredMethod(
            "deleteComment", UUID.class, UUID.class));
    }

    @Test
    void taskDeleteCommentRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(TaskController.class.getDeclaredMethod(
            "deleteComment", UUID.class, UUID.class));
    }

    @Test
    void companyDeleteLogoRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(CompanyController.class.getDeclaredMethod(
            "deleteLogo", UUID.class));
    }

    @Test
    void contactDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(ContactController.class.getDeclaredMethod(
            "delete", UUID.class));
    }

    @Test
    void contactDeletePhotoRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(ContactController.class.getDeclaredMethod(
            "deletePhoto", UUID.class));
    }

    @Test
    void taskDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(TaskController.class.getDeclaredMethod(
            "delete", UUID.class));
    }

    @Test
    void tagDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRequiresAdmin(TagController.class.getDeclaredMethod(
            "delete", UUID.class));
    }

    @Test
    void apiKeyControllerRequiresItAdmin() {
        assertClassHasRequiresItAdmin(ApiKeyController.class);
    }

    @Test
    void webhookControllerRequiresItAdmin() {
        assertClassHasRequiresItAdmin(WebhookController.class);
    }

    @Test
    void brevoSyncControllerRequiresItAdmin() {
        assertClassHasRequiresItAdmin(BrevoSyncController.class);
    }

    @Test
    void auditLogControllerRequiresItAdmin() {
        assertClassHasRequiresItAdmin(AuditLogController.class);
    }

    @Test
    void userControllerListUsersRequiresItAdmin() throws NoSuchMethodException {
        assertHasRequiresItAdmin(UserController.class.getDeclaredMethod(
            "listUsers", Pageable.class));
    }

    private static void assertHasRequiresAdmin(Method method) {
        final RequiresAdmin annotation = method.getAnnotation(RequiresAdmin.class);
        assertNotNull(annotation,
            "Missing @RequiresAdmin on " + method.getDeclaringClass().getSimpleName()
                + "." + method.getName());
    }

    private static void assertHasRequiresItAdmin(Method method) {
        final RequiresItAdmin annotation = method.getAnnotation(RequiresItAdmin.class);
        assertNotNull(annotation,
            "Missing @RequiresItAdmin on " + method.getDeclaringClass().getSimpleName()
                + "." + method.getName());
    }

    private static void assertClassHasRequiresItAdmin(Class<?> controller) {
        final RequiresItAdmin annotation = controller.getAnnotation(RequiresItAdmin.class);
        assertNotNull(annotation,
            "Missing class-level @RequiresItAdmin on " + controller.getSimpleName());
    }

    @Test
    void crmSecurityConfigEnablesMethodSecurity() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(
            org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity.class),
            "SecurityConfig must be annotated with @EnableMethodSecurity to activate @PreAuthorize checks");
    }
}
