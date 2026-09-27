package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.*;
import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.dto.user.UpdateUserRequest;
import com.taja.crm.crm_backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    private String actorId(HttpServletRequest request) {
        return request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();
    }

    @GetMapping
    public PageResponse<UserResponse> findAllUsers(HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.fromPage(userService.findAllUsers(actorId(request), Pagination.of(page, size)));
    }

    @GetMapping("/{id}")
    public UserResponse findByIdUser(HttpServletRequest request, @PathVariable String id) {
        return userService.findByIdUser(actorId(request), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUsers(HttpServletRequest request, @Valid @RequestBody RegisterRequest body) {
        return userService.createUsers(actorId(request), body);
    }

    @PutMapping("/{id}")
    public UserResponse updateUsers(HttpServletRequest request, @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest body) {
        return userService.updateUsers(actorId(request), id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUsers(HttpServletRequest request, @PathVariable String id) {
        userService.deleteUsers(actorId(request), id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "資料重複或仍有關聯資料，無法完成操作");
    }
}
