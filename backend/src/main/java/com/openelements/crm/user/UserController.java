package com.openelements.crm.user;

import com.openelements.crm.security.RequiresItAdmin;
import com.openelements.spring.base.security.user.SystemUser;
import com.openelements.spring.base.security.user.UserDto;
import com.openelements.spring.base.security.user.UserEntity;
import com.openelements.spring.base.security.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
@SecurityRequirement(name = "oidc")
public class UserController {

    private final UserService userService;
    private final AdminUserRepository adminUserRepository;

    public UserController(final UserService userService, final AdminUserRepository adminUserRepository) {
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.adminUserRepository = Objects.requireNonNull(adminUserRepository, "adminUserRepository must not be null");
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public UserDto getMe() {
        return userService.getCurrentUser();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiresItAdmin
    @Operation(summary = "List users", description = "Returns a paginated list of all registered users excluding the SYSTEM-USER. Requires the IT-ADMIN role.")
    public Page<UserDto> listUsers(
        @Parameter(hidden = true)
        @PageableDefault(size = 20) final Pageable pageable) {
        return adminUserRepository.findBySubNot(SystemUser.SUB, pageable)
            .map(this::toDto);
    }

    private UserDto toDto(final UserEntity entity) {
        return new UserDto(
            entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getAvatarUrl(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
