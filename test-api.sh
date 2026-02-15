#!/bin/bash

# API Testing Script
# Tests all API endpoints to ensure they're working correctly

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

API_URL="http://localhost:8080"
PASSED=0
FAILED=0

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       API Endpoint Testing Suite          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""

# Check if API is running
echo -e "${YELLOW}Checking if API server is running...${NC}"
if ! curl -s "$API_URL/health" > /dev/null 2>&1; then
    echo -e "${RED}✗ API server is not running on port 8080${NC}"
    echo ""
    echo "Please start the API server first:"
    echo "  ./run-api.sh"
    echo ""
    echo "Or manually:"
    echo "  mvn exec:java -Dexec.mainClass=\"com.jobqueue.api.JobQueueAPI\""
    exit 1
fi
echo -e "${GREEN}✓ API server is running${NC}"
echo ""

# Test 1: Health Check
echo -e "${YELLOW}[1/5] Testing /health endpoint...${NC}"
HEALTH_RESPONSE=$(curl -s "$API_URL/health")
if echo "$HEALTH_RESPONSE" | jq -e '.status == "healthy"' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Health check passed${NC}"
    echo "  Response: $HEALTH_RESPONSE"
    ((PASSED++))
else
    echo -e "${RED}✗ Health check failed${NC}"
    echo "  Response: $HEALTH_RESPONSE"
    ((FAILED++))
fi
echo ""

# Test 2: Job Submission
echo -e "${YELLOW}[2/5] Testing /api/jobs/submit endpoint...${NC}"
SUBMIT_RESPONSE=$(curl -s -X POST "$API_URL/api/jobs/submit" \
    -H "Content-Type: application/json" \
    -d '{"type":"email","recipient":"test@example.com","subject":"Test","priority":5}')

if echo "$SUBMIT_RESPONSE" | jq -e '.success == true' > /dev/null 2>&1; then
    JOB_ID=$(echo "$SUBMIT_RESPONSE" | jq -r '.jobId')
    echo -e "${GREEN}✓ Job submission successful${NC}"
    echo "  Job ID: $JOB_ID"
    ((PASSED++))
else
    echo -e "${RED}✗ Job submission failed${NC}"
    echo "  Response: $SUBMIT_RESPONSE"
    ((FAILED++))
fi
echo ""

# Test 3: Job Stats
echo -e "${YELLOW}[3/5] Testing /api/jobs/stats endpoint...${NC}"
STATS_RESPONSE=$(curl -s "$API_URL/api/jobs/stats")
if echo "$STATS_RESPONSE" | jq -e 'has("total")' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Job stats retrieved${NC}"
    echo "  Total: $(echo "$STATS_RESPONSE" | jq -r '.total')"
    echo "  Pending: $(echo "$STATS_RESPONSE" | jq -r '.pending')"
    echo "  Completed: $(echo "$STATS_RESPONSE" | jq -r '.completed')"
    ((PASSED++))
else
    echo -e "${RED}✗ Job stats failed${NC}"
    echo "  Response: $STATS_RESPONSE"
    ((FAILED++))
fi
echo ""

# Test 4: System Metrics
echo -e "${YELLOW}[4/5] Testing /api/metrics endpoint...${NC}"
METRICS_RESPONSE=$(curl -s "$API_URL/api/metrics")
if echo "$METRICS_RESPONSE" | jq -e 'has("jobsProcessed")' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ System metrics retrieved${NC}"
    echo "  Jobs Processed: $(echo "$METRICS_RESPONSE" | jq -r '.jobsProcessed')"
    echo "  Success Rate: $(echo "$METRICS_RESPONSE" | jq -r '.successRate')%"
    echo "  Throughput: $(echo "$METRICS_RESPONSE" | jq -r '.throughput') jobs/sec"
    ((PASSED++))
else
    echo -e "${RED}✗ System metrics failed${NC}"
    echo "  Response: $METRICS_RESPONSE"
    ((FAILED++))
fi
echo ""

# Test 5: Submit Multiple Job Types
echo -e "${YELLOW}[5/5] Testing multiple job types...${NC}"
JOB_TYPES=("trade_settlement" "rtl_synthesis" "vulnerability_scan")
BATCH_PASSED=0
BATCH_FAILED=0

for job_type in "${JOB_TYPES[@]}"; do
    RESPONSE=$(curl -s -X POST "$API_URL/api/jobs/submit" \
        -H "Content-Type: application/json" \
        -d "{\"type\":\"$job_type\",\"priority\":7}")
    
    if echo "$RESPONSE" | jq -e '.success == true' > /dev/null 2>&1; then
        ((BATCH_PASSED++))
    else
        ((BATCH_FAILED++))
    fi
done

if [ $BATCH_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All job types submitted successfully${NC}"
    echo "  Submitted: ${JOB_TYPES[*]}"
    ((PASSED++))
else
    echo -e "${RED}✗ Some job types failed ($BATCH_FAILED/${#JOB_TYPES[@]})${NC}"
    ((FAILED++))
fi
echo ""

# Summary
echo -e "${BLUE}════════════════════════════════════════════${NC}"
echo -e "${BLUE}            TEST SUMMARY                    ${NC}"
echo -e "${BLUE}════════════════════════════════════════════${NC}"
echo ""
echo -e "Total Tests:   ${BLUE}5${NC}"
echo -e "Passed:        ${GREEN}$PASSED${NC}"
echo -e "Failed:        ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ ALL API TESTS PASSED!${NC}"
    echo ""
    echo "Your API is working perfectly. You can now:"
    echo "1. Open the dashboard: src/main/resources/static/dashboard.html"
    echo "2. Test the UI by submitting jobs"
    echo "3. Proceed with deployment"
    exit 0
else
    echo -e "${RED}✗ SOME API TESTS FAILED${NC}"
    echo ""
    echo "Check the API server logs for errors."
    exit 1
fi