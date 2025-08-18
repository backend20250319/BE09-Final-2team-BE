package com.momnect.userservice.command.service;

import com.momnect.userservice.command.dto.UserDTO;
import com.momnect.userservice.command.entity.User;
import com.momnect.userservice.command.repository.UserRepository;
import com.momnect.userservice.exception.DuplicateUserException;
import com.momnect.userservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. 로그아웃 (RefreshToken 케저)
    public void logout(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        user.setRefreshToken(null);
        user.setUpdatedBy(userId);
        userRepository.save(user);
    }

    // 2. 마이페이지 조회
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getIsDeleted()) {
            throw new UserNotFoundException("탈퇴한 사용자입니다.");
        }

        return convertToUserDTO(user);
    }

    // 3. 프로필 수정
    public UserDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("탈퇴한 사용자입니다."));

        if (user.getIsDeleted()) {
            throw new UserNotFoundException("탈퇴한 사용자입니다.");
        }

        // 닉네임 중복 검사 (본인 제외)
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNicknameAndIdNot(request.getNickname(), userId)) {
                throw new DuplicateUserException("이미 사용 중인 닉네임입니다.");
            }
        }

        // 프로필 업데이트
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) {
            String cleanPhoneNumber = request.getPhoneNumber().replaceAll("[^0-9]","");
            user.setPhoneNumber(cleanPhoneNumber);
        }

        user.setUpdatedBy(userId);
        User savedUser = userRepository.save(user);

        return convertToUserDTO(savedUser);
    }
}
