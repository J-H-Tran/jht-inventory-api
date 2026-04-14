package com.jht.nvntry.movements;

import com.jht.nvntry.movements.model.request.InventoryMovementRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class InventoryMovementValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return InventoryMovementRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        InventoryMovementRequest request = (InventoryMovementRequest) target;

        /* Guard: if core fields failed @NotNull, Bean Validation already registered those errors.
         * Exit here - further validation would NPE.
         * This is the correct pattern when combining Spring's Validator interface with Bean Validation.
         * The Validator runs after @Valid populates BindingResult, but it does not know which fields already
         * failed. Guarding against null before accessing fields that could be null is my responsibility.
         * */
        if (request.movementType() == null || request.quantityDelta() == null) return;

        // Movement type specific rules
        switch(request.movementType()) {
            case ADJUST -> {
                if (request.reasonCode() == null || request.reasonCode().isBlank()) {
                    errors.rejectValue("reasonCode", "reasonCode.required",
                            "ADJUST movement requires a reason code");
                }
                if (request.quantityDelta() == 0) {
                    errors.rejectValue("quantityDelta", "quantityDelta.nonzero",
                            "ADJUST movement must have non-zero quantity delta");
                }
            }
            case RECEIVE -> {
                if (request.reasonCode() != null && !request.reasonCode().isBlank()) {
                    errors.rejectValue("reasonCode", "reasonCode.notAllowed",
                            "RECEIVE movement should not have a reason code");
                }
                if (request.quantityDelta() <= 0) {
                    errors.rejectValue("quantityDelta", "quantityDelta.positive",
                            "RECEIVE movement must have positive quantity delta");
                }
            }
            case SHIP -> {
                if (request.reasonCode() != null && !request.reasonCode().isBlank()) {
                    errors.rejectValue("reasonCode", "reasonCode.notAllowed",
                            "SHIP movement should not have a reason code");
                }
                if (request.quantityDelta() >= 0) {
                    errors.rejectValue("quantityDelta", "quantityDelta.negative",
                            "SHIP movement must have negative quantity delta");
                }
            }
        }

        // Reference consistency
        if (
            (request.referenceId() == null && request.referenceType() != null) ||
            (request.referenceId() != null && request.referenceType() == null)
        ) {
            errors.rejectValue("referenceId", "reference.pair",
                    "Both referenceId and referenceType must be present or both absent");
        }
    }
    // TODO: return 404 if reasonCode is supplied but does not exist in adjustment_reasons
    // TODO: return 422 if submitting SHIP would result in negative stock
}