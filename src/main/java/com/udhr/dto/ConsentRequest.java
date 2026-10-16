package com.udhr.dto;

import lombok.Data;

@Data
public class ConsentRequest {
    private String identifier;           // ID number, MRN or passport number
    private String consentGivenBy;       // "Patient", or the parent/guardian for a child
    private Boolean smsRemindersConsent;
}
