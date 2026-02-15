#!/bin/bash

# Cloudflare R2 Environment Variables Setup Script
# Usage: source setup-r2-env.sh
# Or: ./setup-r2-env.sh (if executable)

# Cloudflare R2 S3 Credentials
export CLOUDFLARE_R2_ACCESS_KEY_ID="bbee0c025a22819059074e406fca6282"
export CLOUDFLARE_R2_SECRET_ACCESS_KEY="a1bfb5509129c1dedeb3985d3def93def629a467714709b65d3c98cadbf122fb"
export CLOUDFLARE_R2_ENDPOINT="https://2d05daa266a709a4f14708966c3b1539.r2.cloudflarestorage.com"

# Cloudflare API Token (for API operations if needed)
export CLOUDFLARE_API_TOKEN="eDyGdRHciEuVrLK9UtIbXQVmK3_Ki-sHXpYimI2o"

# R2 Bucket Name (update this with your actual bucket name)
export CLOUDFLARE_R2_BUCKET_NAME="vidopi"

# Optional: Public URL base if you have a custom domain configured
# export CLOUDFLARE_R2_PUBLIC_URL_BASE="https://your-custom-domain.com"

echo "✅ Cloudflare R2 environment variables have been set:"
echo "   - CLOUDFLARE_R2_ACCESS_KEY_ID: ${CLOUDFLARE_R2_ACCESS_KEY_ID:0:10}..."
echo "   - CLOUDFLARE_R2_SECRET_ACCESS_KEY: ${CLOUDFLARE_R2_SECRET_ACCESS_KEY:0:10}..."
echo "   - CLOUDFLARE_R2_ENDPOINT: $CLOUDFLARE_R2_ENDPOINT"
echo "   - CLOUDFLARE_R2_BUCKET_NAME: $CLOUDFLARE_R2_BUCKET_NAME"
echo "   - CLOUDFLARE_API_TOKEN: ${CLOUDFLARE_API_TOKEN:0:10}..."
echo ""
echo "⚠️  Note: Make sure to set CLOUDFLARE_R2_BUCKET_NAME to your actual bucket name!"
