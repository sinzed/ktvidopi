#!/bin/bash

# Upload Video to R2 - Test Script
# This script uploads a video file to the Cloudflare R2 storage via the upload API

# Configuration
API_URL="http://localhost:8080/api/upload/video"
VIDEO_FILE="./example.mp4"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Uploading video to R2...${NC}"
echo "API URL: $API_URL"
echo "Video file: $VIDEO_FILE"
echo ""

# Check if file exists
if [ ! -f "$VIDEO_FILE" ]; then
    echo -e "${RED}Error: Video file not found: $VIDEO_FILE${NC}"
    exit 1
fi

# Upload the file
echo -e "${YELLOW}Uploading...${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
    -F "file=@$VIDEO_FILE")

# Extract HTTP status code (last line)
http_code=$(echo "$response" | tail -n1)
# Extract response body (all but last line)
response_body=$(echo "$response" | sed '$d')

echo ""
if [ "$http_code" -eq 200 ]; then
    echo -e "${GREEN}✓ Upload successful!${NC}"
    echo -e "${GREEN}HTTP Status: $http_code${NC}"
    echo ""
    echo "Response:"
    echo "$response_body" | jq '.' 2>/dev/null || echo "$response_body"
else
    echo -e "${RED}✗ Upload failed!${NC}"
    echo -e "${RED}HTTP Status: $http_code${NC}"
    echo ""
    echo "Response:"
    echo "$response_body" | jq '.' 2>/dev/null || echo "$response_body"
    exit 1
fi
