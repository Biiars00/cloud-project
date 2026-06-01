package com.tech.feedback_platform.controller;

import com.tech.feedback_platform.dto.FeedbackRequest;
import com.tech.feedback_platform.dto.FeedbackResponse;
import com.tech.feedback_platform.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse criar(
            @RequestBody @Valid FeedbackRequest request) {

        return feedbackService.criar(request);
    }

    @GetMapping
    public List<FeedbackResponse> listar() {
        return feedbackService.listar();
    }
}
