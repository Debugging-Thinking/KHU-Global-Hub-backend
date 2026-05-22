package com.khu.globalhub.domain.member.dto;

import com.khu.globalhub.shared.enums.MentoringRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMentoringRoleRequest(

        @NotNull
        MentoringRole mentoringRole
) {}
