package com.finpath.backend.dto;
import jakarta.validation.constraints.*;
public final class AuthDtos {
  private AuthDtos(){}
  public record RegisterRequest(@NotBlank @Size(min=2,max=120) String fullName,@NotBlank @Email String email,@NotBlank @Size(min=6,max=100) String password){}
  public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
  public record AuthResponse(String token, UserResponse user){}
  public record UserResponse(String id,String email,String fullName){}
  public record ForgotPasswordRequest(@NotBlank @Email String email){}
}
