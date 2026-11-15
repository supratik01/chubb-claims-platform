package com.chubb.claims.infrastructure.seed;

import com.chubb.claims.domain.model.AppUser;
import com.chubb.claims.domain.model.LineOfBusiness;
import com.chubb.claims.domain.model.Policy;
import com.chubb.claims.domain.model.Role;
import com.chubb.claims.domain.repository.PolicyRepository;
import com.chubb.claims.domain.repository.UserRepository;
import com.chubb.claims.service.Actor;
import com.chubb.claims.service.ClaimSubmissionService;
import com.chubb.claims.service.ClaimWorkflowService;
import com.chubb.claims.service.command.SubmitClaimCommand;
import com.chubb.claims.service.port.PasswordHasher;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fills an empty database with demo users, policies and claims in every stage of the workflow.
 * Active only under the "demo" profile.
 *
 * <p>Claims are created through the same services the API uses, so each one has a real status history and
 * real events; the exposure summary is then built by the Kafka consumer exactly as it would be in use.
 */
@Component
@Profile("demo")
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final PolicyRepository policies;
    private final PasswordHasher passwordHasher;
    private final ClaimSubmissionService submission;
    private final ClaimWorkflowService workflow;
    private final DemoProperties properties;
    private final Clock clock;

    DemoDataSeeder(UserRepository users, PolicyRepository policies, PasswordHasher passwordHasher,
                   ClaimSubmissionService submission, ClaimWorkflowService workflow, DemoProperties properties,
                   Clock clock) {
        this.users = users;
        this.policies = policies;
        this.passwordHasher = passwordHasher;
        this.submission = submission;
        this.workflow = workflow;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByUsername("claimant.mei").isPresent()) {
            log.info("Demo data already present; nothing to seed");
            return;
        }
        LocalDate today = LocalDate.now(clock);

        AppUser mei = user("claimant.mei", "Mei Ling Tan", Role.CLAIMANT);
        AppUser jack = user("claimant.jack", "Jack Wilson", Role.CLAIMANT);
        AppUser sam = user("officer.sam", "Sam Lee", Role.CLAIMS_OFFICER);
        AppUser priya = user("officer.priya", "Priya Nair", Role.CLAIMS_OFFICER);
        user("manager.kenji", "Kenji Sato", Role.CLAIMS_MANAGER);

        Policy meiMotor = policy("POL-SG-M-10021", mei, LineOfBusiness.MOTOR, "SG", "SGD", "80000.00", today);
        Policy meiHome = policy("POL-SG-P-20417", mei, LineOfBusiness.PROPERTY, "SG", "SGD", "500000.00", today);
        Policy jackMotor = policy("POL-AU-M-30388", jack, LineOfBusiness.MOTOR, "AU", "AUD", "60000.00", today);
        Policy jackHome = policy("POL-AU-P-40952", jack, LineOfBusiness.PROPERTY, "AU", "AUD", "750000.00", today);

        Actor meiActor = actor(mei);
        Actor jackActor = actor(jack);
        Actor samActor = actor(sam);
        Actor priyaActor = actor(priya);

        // 1. New and unassigned: waiting in the queue.
        submit(meiActor, meiMotor, today.minusDays(1), "Bukit Timah Road, Singapore",
                "Another car reversed into my parked car and damaged the front bumper and headlight.", "4200.00");

        // 2. Waiting on the claimant.
        UUID waterDamage = submit(meiActor, meiHome, today.minusDays(9), "Tampines, Singapore",
                "A burst pipe in the kitchen flooded the floor and damaged the cabinets.", "18500.00");
        workflow.pickUp(samActor, waterDamage);
        workflow.requestInformation(samActor, waterDamage,
                "Please describe the plumber's findings and the date the pipe was last serviced.");

        // 3. Under review by an officer.
        UUID collision = submit(jackActor, jackMotor, today.minusDays(6), "Parramatta Road, Sydney",
                "Collision at an intersection; the other driver ran a red light. Police report filed.", "7800.00");
        workflow.pickUp(priyaActor, collision);

        // 4. Approved, payment not yet recorded.
        UUID stormDamage = submit(jackActor, jackHome, today.minusDays(20), "Newtown, Sydney",
                "Storm damage to the roof and water ingress into two bedrooms.", "32000.00");
        workflow.pickUp(samActor, stormDamage);
        workflow.approve(samActor, stormDamage, new BigDecimal("28000.00"), "Approved less the policy excess.");

        // 5. Rejected.
        UUID scratch = submit(jackActor, jackMotor, today.minusDays(30), "Bondi, Sydney",
                "Scratches along the driver side found after parking overnight.", "1500.00");
        workflow.pickUp(priyaActor, scratch);
        workflow.reject(priyaActor, scratch, "Wear and tear is excluded under section 4.2 of the policy.");

        // 6. Settled.
        UUID windscreen = submit(meiActor, meiMotor, today.minusDays(45), "Pan Island Expressway, Singapore",
                "Windscreen cracked by debris from a lorry on the expressway.", "2300.00");
        workflow.pickUp(samActor, windscreen);
        workflow.approve(samActor, windscreen, new BigDecimal("2300.00"), "Approved in full.");
        workflow.settle(samActor, windscreen);

        log.info("Seeded demo data: 5 users, 4 policies, 6 claims");
    }

    private AppUser user(String username, String displayName, Role role) {
        return users.save(new AppUser(UUID.randomUUID(), username, passwordHasher.hash(properties.userPassword()),
                displayName, role));
    }

    private Policy policy(String number, AppUser holder, LineOfBusiness line, String country, String currency,
                          String sumInsured, LocalDate today) {
        return policies.save(new Policy(UUID.randomUUID(), number, holder, line, country, currency,
                new BigDecimal(sumInsured), today.minusDays(200), today.plusDays(165)));
    }

    private UUID submit(Actor claimant, Policy policy, LocalDate incidentDate, String location, String description,
                        String amount) {
        return submission.submit(claimant,
                new SubmitClaimCommand(policy.getId(), incidentDate, location, description, new BigDecimal(amount)))
                .summary().id();
    }

    private static Actor actor(AppUser user) {
        return new Actor(user.getId(), user.getRole());
    }
}
