package com.goormthon.everytime.app.service.board;

import com.goormthon.everytime.app.domain.board.Board;
import com.goormthon.everytime.app.domain.board.post.Post;
import com.goormthon.everytime.app.domain.board.post.PostImage;
import com.goormthon.everytime.app.domain.image.Image;
import com.goormthon.everytime.app.domain.user.User;
import com.goormthon.everytime.app.dto.board.reqDto.PostReqDto;
import com.goormthon.everytime.app.repository.*;
import com.goormthon.everytime.global.exception.CustomException;
import com.goormthon.everytime.global.exception.code.ErrorCode;
import com.goormthon.everytime.global.exception.code.SuccessCode;
import com.goormthon.everytime.global.template.ApiResTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApiResTemplate<Void> uploadPost(
            PostReqDto reqDto, Principal principal, Long boardId) {

        Long userId = Long.parseLong(principal.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND, ErrorCode.BOARD_NOT_FOUND.getMessage()));

        Post post = postRepository.save(reqDto.toEntity(board, user));

        if (reqDto.files() != null && !reqDto.files().isEmpty()) {
            saveImages(reqDto.files(), post);
        }

        return ApiResTemplate.success(SuccessCode.POST_UPLOAD_SUCCESS, null);
    }

    private void saveImages(List<MultipartFile> files, Post post) {
        for (MultipartFile file : files) {
            if (!isValidImageExtension(file)) {
                throw new CustomException(ErrorCode.INVALID_FILE_TYPE, ErrorCode.INVALID_FILE_TYPE.getMessage());
            }

            String imageUrl = file.getOriginalFilename();

            Image image = imageRepository.save(Image.builder()
                    .imageUrl(imageUrl)
                    .build());

            postImageRepository.save(PostImage.builder()
                    .post(post)
                    .image(image)
                    .build());
        }
    }

    private boolean isValidImageExtension(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        return IMAGE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
