package com.khu.globalhub.domain.member.dto;

import com.khu.globalhub.global.enums.MentoringRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMentoringRoleRequest(

        @NotNull
        MentoringRole mentoringRole
) {}
