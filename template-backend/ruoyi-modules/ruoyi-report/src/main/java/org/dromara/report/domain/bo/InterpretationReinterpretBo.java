package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InterpretationReinterpretBo {
    @NotNull
    @Positive
    private Long analysisId;
    @NotNull
    @Positive
    private Long reportId;
}
