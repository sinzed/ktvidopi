# Video Upload to R2 Setup

## Prerequisites

1. **Create a bucket in Cloudflare R2:**
   - Go to Cloudflare Dashboard → R2
   - Create a bucket (e.g., "vidopi")
   - Note the bucket name

## Running the Application

### Option 1: Source environment variables before starting

```bash
# Source the environment variables
source setup-r2-env.sh

# Make sure the bucket name matches your R2 bucket
export CLOUDFLARE_R2_BUCKET_NAME=your-bucket-name

# Run the application
./gradlew bootRun
```

### Option 2: Set environment variables in your IDE

If running from IntelliJ IDEA or another IDE:
1. Go to Run → Edit Configurations
2. Add environment variables:
   - `CLOUDFLARE_R2_ACCESS_KEY_ID=bbee0c025a22819059074e406fca6282`
   - `CLOUDFLARE_R2_SECRET_ACCESS_KEY=a1bfb5509129c1dedeb3985d3def93def629a467714709b65d3c98cadbf122fb`
   - `CLOUDFLARE_R2_ENDPOINT=https://2d05daa266a709a4f14708966c3b1539.r2.cloudflarestorage.com`
   - `CLOUDFLARE_R2_BUCKET_NAME=your-bucket-name` (replace with your actual bucket name)

### Option 3: Update application.properties

You can also set the bucket name directly in `application.properties`:
```properties
cloudflare.r2.bucket-name=your-bucket-name
```

## Testing the Upload

After starting the application with the correct environment variables:

```bash
cd test/api
./upload-video.sh
```

## About the returned URL (important)

Cloudflare R2's `*.r2.cloudflarestorage.com` hostname is an **S3 API endpoint**, not a public asset URL. That means:

- A URL like `https://<bucket>.r2.cloudflarestorage.com/<key>` will typically **NOT** be accessible in a browser without signing/auth.
- The upload API now returns a **presigned** `downloadUrl` (and `url` for backwards compatibility), which *is* accessible for a limited time.

### (Optional) Make objects publicly accessible via a public base URL

If you have configured public access (for example via a custom domain) you can set:

- `CLOUDFLARE_R2_PUBLIC_URL_BASE=https://your-public-domain`

Then the API will also return `publicUrl`.

## Troubleshooting

### Error: "The specified bucket does not exist"
- Verify the bucket exists in your Cloudflare R2 dashboard
- Check that `CLOUDFLARE_R2_BUCKET_NAME` matches the exact bucket name (case-sensitive)
- Make sure the environment variable is set before starting the application

### Check which bucket name is being used
- Check the application logs - it will show: "Using bucket name from config: <bucket-name>"
