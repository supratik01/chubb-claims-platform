package com.chubb.claims.api;

import com.chubb.claims.api.dto.PolicyResponse;
import com.chubb.claims.service.Actor;
import com.chubb.claims.service.PolicyService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policies;

    public PolicyController(PolicyService policies) {
        this.policies = policies;
    }

    @GetMapping
    public List<PolicyResponse> listMyPolicies(Actor actor) {
        return policies.listHeldBy(actor).stream().map(PolicyResponse::from).toList();
    }
}
