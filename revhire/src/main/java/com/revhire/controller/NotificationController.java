package com.revhire.controller;

import com.revhire.model.Notification;
import com.revhire.model.User;
import com.revhire.repository.NotificationRepository;
import com.revhire.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/notifications")
    public String notifications(Model model) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email).orElse(null);

        List<Notification> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        model.addAttribute("notifications", notifications);

        return "notifications/list";
    }
    
    @org.springframework.web.bind.annotation.PostMapping("/notifications/mark-all-read")
    public String markAllAsRead() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null) {
            notificationRepository.markAllAsRead(user.getId());
        }
        
        return "redirect:/notifications";
    }
}