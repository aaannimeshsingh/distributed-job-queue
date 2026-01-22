package com.jobqueue;

/**
 * Enumeration of supported job types across different domains
 * Demonstrates understanding of Financial, Hardware, and Security workflows
 */
public enum JobType {
    // Generic jobs
    GENERIC("generic", "Generic task processing", JobDomain.GENERIC),
    
    // Financial domain (Fidelity)
    TRADE_SETTLEMENT("trade_settlement", "Process trade settlements", JobDomain.FINANCIAL),
    RISK_CALCULATION("risk_calculation", "Calculate portfolio risk metrics", JobDomain.FINANCIAL),
    FRAUD_DETECTION("fraud_detection", "Analyze transactions for fraud", JobDomain.FINANCIAL),
    REGULATORY_REPORT("regulatory_report", "Generate compliance reports", JobDomain.FINANCIAL),
    MARKET_DATA_ANALYSIS("market_data_analysis", "Analyze market data feeds", JobDomain.FINANCIAL),
    
    // Hardware domain (Intel)
    RTL_SYNTHESIS("rtl_synthesis", "Synthesize RTL design", JobDomain.HARDWARE),
    TIMING_ANALYSIS("timing_analysis", "Static timing analysis", JobDomain.HARDWARE),
    POWER_ANALYSIS("power_analysis", "Power consumption analysis", JobDomain.HARDWARE),
    PLACE_AND_ROUTE("place_and_route", "Place and route optimization", JobDomain.HARDWARE),
    DRC_CHECK("drc_check", "Design rule checking", JobDomain.HARDWARE),
    VERIFICATION("verification", "Functional verification", JobDomain.HARDWARE),
    
    // Security domain (Intel Security)
    VULNERABILITY_SCAN("vulnerability_scan", "Scan for vulnerabilities", JobDomain.SECURITY),
    LOG_ANALYSIS("log_analysis", "Analyze security logs", JobDomain.SECURITY),
    THREAT_DETECTION("threat_detection", "Detect security threats", JobDomain.SECURITY),
    COMPLIANCE_CHECK("compliance_check", "Check security compliance", JobDomain.SECURITY),
    INCIDENT_RESPONSE("incident_response", "Handle security incidents", JobDomain.SECURITY),
    MALWARE_ANALYSIS("malware_analysis", "Analyze malware samples", JobDomain.SECURITY),
    
    // Legacy support
    EMAIL("email", "Send email notifications", JobDomain.GENERIC),
    PROCESS_DATA("process_data", "Process data", JobDomain.GENERIC),
    GENERATE_REPORT("generate_report", "Generate reports", JobDomain.GENERIC);

    private final String code;
    private final String description;
    private final JobDomain domain;

    JobType(String code, String description, JobDomain domain) {
        this.code = code;
        this.description = description;
        this.domain = domain;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public JobDomain getDomain() {
        return domain;
    }

    public static JobType fromCode(String code) {
        for (JobType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return GENERIC;
    }

    public enum JobDomain {
        GENERIC("Generic"),
        FINANCIAL("Financial Services"),
        HARDWARE("Hardware/EDA"),
        SECURITY("Security/Cybersecurity");

        private final String displayName;

        JobDomain(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}