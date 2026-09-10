# Admin access creating base resources
ADMIN_CLIENT_ID=root
ADMIN_CLIENT_SECRET=s3cr3t

echo "Obtaining root access token..."
TOKEN_RESPONSE=$(curl --fail-with-body -s -S -X POST http://localhost:8181/api/catalog/v1/oauth/tokens \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d "grant_type=client_credentials&client_id=${ADMIN_CLIENT_ID}&client_secret=${ADMIN_CLIENT_SECRET}&scope=PRINCIPAL_ROLE:ALL" 2>&1) || {
    echo "❌ Failed to obtain access token"
    echo "$TOKEN_RESPONSE" >&2
    return
}

TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "❌ Failed to parse access token from response"
    echo "$TOKEN_RESPONSE"
    return
fi
echo "✅ Obtained access token"