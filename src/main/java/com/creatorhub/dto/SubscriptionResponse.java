package com.creatorhub.dto;

import com.creatorhub.model.enums.SubStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long id;
    private Long fanId;
    private String fanUsername;
    private Long tierId;
    private String tierName;

    /** The creator who owns the tier. */
    private Long creatorId;

    private LocalDate startDate;
    private SubStatus status;
}
