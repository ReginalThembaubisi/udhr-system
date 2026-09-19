package com.udhr.dto;

import com.udhr.model.Referral;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReferralReportResponse {
    private String facilityName;
    private int totalOutgoing;
    private int totalIncoming;
    private int pendingIncoming;
    private int emergencyReferrals;
    private List<FacilityReferralTally> topDestinations;
    private List<FacilityReferralTally> topSources;
    private List<Referral> recentActivity;
}
