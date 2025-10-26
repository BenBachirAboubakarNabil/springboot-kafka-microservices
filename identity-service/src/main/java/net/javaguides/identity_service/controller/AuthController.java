package net.javaguides.identity_service.controller;



import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.dto.AuthRequest;
import net.javaguides.identity_service.dto.SignUpRequest;
import net.javaguides.identity_service.dto.UserDto;
import net.javaguides.identity_service.exception.AuthException;
import net.javaguides.identity_service.metrics.AuthenticationMetrics;
import net.javaguides.identity_service.service.AuthService;
import net.javaguides.identity_service.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuthenticationMetrics authenticationMetrics;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> addNewUser(@RequestBody SignUpRequest signUpRequest) {
        log.info("start of user registration");
        try {
            String message = authService.saveUser(signUpRequest);
            ApiResponse<String> apiResponse = new ApiResponse<>(message, HttpStatus.CREATED.value());
            return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
        }
        catch(AuthException e){
            ApiResponse<String> apiResponse = new ApiResponse<>(e.getMessage(), e.getStatus().value());
            log.warn("User registration failed: {}", e.getMessage());
            return new ResponseEntity<>(apiResponse, e.getStatus());
        }
        catch(Exception e){
            ApiResponse<String> apiResponse = new ApiResponse<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
            log.error("An unexpected error occurred during user registration: {}", e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<String>> getToken(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        log.info("start of token generation for user");
        boolean authSuccess = false;
        String failureReason = "UNKNOWN_ERROR";
        try {
            Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
            if (authenticate.isAuthenticated()) {
                authSuccess = true;

                String generateToken = authService.generateToken(authRequest, response);

                ApiResponse<String> apiResponse = new ApiResponse<>(generateToken, HttpStatus.OK.value());
                return new ResponseEntity<>(apiResponse, HttpStatus.OK);
            } else {
                failureReason = "AUTH_NOT_AUTHENTICATED";
                ApiResponse<String> apiResponse = new ApiResponse<>("Invalid access!", HttpStatus.BAD_REQUEST.value());
                log.warn("Token generation failed");
                return new ResponseEntity<>(apiResponse, HttpStatus.OK);            }
        }catch(Exception e){
            failureReason = "INVALID_CREDENTIALS";
            ApiResponse<String> apiResponse = new ApiResponse<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
            log.error("An unexpected error occurred during token generation: {}", e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }finally {
            if (!authSuccess) {
                authenticationMetrics.recordAuthenticationOutcome(false, failureReason);
            } else {
                authenticationMetrics.recordAuthenticationOutcome(true, "N/A");
            }
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validateToken(@RequestParam("token") String token) {
        log.info("start of token validation");
        try {
            authService.validateToken(token);
            ApiResponse<String> apiResponse = new ApiResponse<>("Token is valid", HttpStatus.OK.value());
            return new ResponseEntity<>(apiResponse, HttpStatus.OK);
        }catch(Exception e){
            ApiResponse<String> apiResponse = new ApiResponse<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
            log.error("An unexpected error occurred during token validation: {}", e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(@AuthenticationPrincipal UserDetails currentUser) {
        log.info("start of fetching current user details");
        try {
            UserDto userDto = userService.getUserByUsername(currentUser.getUsername());
            ApiResponse<UserDto> apiResponse = new ApiResponse<>(userDto, HttpStatus.OK.value());
            return new ResponseEntity<>(apiResponse, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<String> apiResponse = new ApiResponse<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
            log.error("An unexpected error occurred while fetching current user details: {}", e.getMessage());
            return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
