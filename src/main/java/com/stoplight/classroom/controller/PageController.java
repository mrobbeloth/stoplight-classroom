package com.stoplight.classroom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/join")
    public String studentJoin() { return "student-join"; }

    @GetMapping("/session/{id}")
    public String studentSession(@PathVariable Long id) { return "student-session"; }

    @GetMapping("/login")
    public String teacherLogin() { return "teacher-login"; }

    @GetMapping("/signup")
    public String teacherSignup() { return "teacher-signup"; }

    @GetMapping("/dashboard")
    public String dashboard() { return "teacher-dashboard"; }

    @GetMapping("/dashboard/session/{id}")
    public String liveSession(@PathVariable Long id) { return "teacher-session"; }

    @GetMapping("/dashboard/stats")
    public String stats() { return "teacher-stats"; }

    @GetMapping("/admin/teacher-signups")
    public String adminTeacherSignups() { return "admin-teacher-signups"; }
}
