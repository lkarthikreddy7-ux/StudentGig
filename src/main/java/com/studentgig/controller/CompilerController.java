package com.studentgig.controller;

import com.studentgig.service.CodeExecutionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/compiler")
public class CompilerController {
    private final CodeExecutionService executor;
    public CompilerController(CodeExecutionService executor) { this.executor = executor; }

    private boolean logged(HttpSession s) { return s.getAttribute("userId") != null; }

    @PostMapping("/run")
    public Object run(@RequestBody(required=false) Map<String,Object> body, HttpSession session) {
        if (!logged(session)) return Map.of("error", "Please login first");
        if (body == null) return Map.of("error", "Compiler request is empty");
        String language = String.valueOf(body.getOrDefault("language", "java"));
        String code = String.valueOf(body.getOrDefault("code", ""));
        String input = String.valueOf(body.getOrDefault("input", ""));
        var r = executor.execute(language, code, input);
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("success", r.success());
        out.put("stage", r.stage());
        out.put("output", r.output());
        if (!r.success()) out.put("error", r.output());
        return out;
    }

    @PostMapping("/run-tests")
    public Object runTests(@RequestBody(required=false) Map<String,Object> body, HttpSession session) {
        if (!logged(session)) return Map.of("error", "Please login first");
        if (body == null) return Map.of("error", "Compiler request is empty");
        Object raw = body.get("testCases");
        if (!(raw instanceof List<?> tests) || tests.isEmpty()) return Map.of("error", "Add at least one test case");
        if (tests.size() > 20) return Map.of("error", "Maximum 20 test cases per run");
        String language = String.valueOf(body.getOrDefault("language", "java"));
        String code = String.valueOf(body.getOrDefault("code", ""));
        List<Map<String,Object>> results = new ArrayList<>();
        int passed = 0;
        for (Object item : tests) {
            if (!(item instanceof Map<?,?> m)) continue;
            String input = String.valueOf(m.containsKey("input") ? m.get("input") : "");
            String expected = String.valueOf(m.containsKey("output") ? m.get("output") : "");
            var r = executor.execute(language, code, input);
            boolean ok = r.success() && executor.sameOutput(expected, r.output());
            if (ok) passed++;
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("passed", ok); row.put("success", r.success()); row.put("stage", r.stage());
            row.put("expected", expected); row.put("output", r.output());
            results.add(row);
        }
        return Map.of("success", !results.isEmpty() && passed == results.size(), "passed", passed, "total", results.size(), "results", results);
    }
}
