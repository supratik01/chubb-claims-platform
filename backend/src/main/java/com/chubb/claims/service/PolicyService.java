package com.chubb.claims.service;

import com.chubb.claims.domain.repository.PolicyRepository;
import com.chubb.claims.service.view.PolicyView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

    private final PolicyRepository policies;

    public PolicyService(PolicyRepository policies) {
        this.policies = policies;
    }

    @Transactional(readOnly = true)
    public List<PolicyView> listHeldBy(Actor actor) {
        return policies.findByHolderIdOrderByPolicyNumber(actor.userId()).stream().map(PolicyView::of).toList();
    }
}
