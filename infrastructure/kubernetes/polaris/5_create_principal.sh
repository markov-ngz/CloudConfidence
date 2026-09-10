REALM=POLARIS

ADMIN_CLIENT_ID=root
ADMIN_CLIENT_SECRET=s3cr3t

PRINCIPAL=tma_user
PRINCIPAL_ROLE=rf_dev_elt

# NOTE : here the fluss user have full catalog management which is unsecure for production
# It is clearly done to avoid to have fine management of role & permissions as it was not the object of this project
# For production it should only need to have rw access over specified namespaces

# 1. Creating Principal
echo "Creating principal $PRINCIPAL..."
PRINCIPAL_RESPONSE=$(curl --fail-with-body -s -X POST http://localhost:8181/api/management/v1/principals \
    -H "Authorization: Bearer $TOKEN" \
    -H "Polaris-Realm: $REALM" \
    -H "Content-Type: application/json" \
    -d '{"principal": {"name": "'$PRINCIPAL'", "properties": {}}}' 2>&1) || {
    echo "❌ Failed to create principal"
    echo "$PRINCIPAL_RESPONSE" >&2
    exit 1
}

# 2. Fetching his creds
USER_CLIENT_ID=$(echo $PRINCIPAL_RESPONSE | jq -r '.credentials.clientId')
USER_CLIENT_SECRET=$(echo $PRINCIPAL_RESPONSE | jq -r '.credentials.clientSecret')
if [ -z "$USER_CLIENT_ID" ] || [ "$USER_CLIENT_ID" = "null" ] || [ -z "$USER_CLIENT_SECRET" ] || [ "$USER_CLIENT_SECRET" = "null" ]; then
    echo "❌ Failed to parse user credentials from response"
    echo "$PRINCIPAL_RESPONSE"
    exit 1
fi
echo "✅ Principal created with clientId: $USER_CLIENT_ID"


# 3. Granting the principal role
echo "Assigning principal role to principal..."
RESPONSE=$(curl --fail-with-body -s -S -X PUT http://localhost:8181/api/management/v1/principals/$PRINCIPAL/principal-roles \
    -H "Authorization: Bearer $TOKEN" \
    -H "Polaris-Realm: $REALM" \
    -H "Content-Type: application/json" \
    -d '{"principalRole": {"name": "'$PRINCIPAL_ROLE'"}}' 2>&1) && echo -n "" || {
    echo "❌ Failed to assign principal role"
    echo "$RESPONSE" >&2
    exit 1
}
echo "✅ Principal role assigned"



echo "Saving Polaris Credentials :"
echo Client ID : $USER_CLIENT_ID
echo Client Secret : $USER_CLIENT_SECRET

echo "Fluss credential setting :"
echo "  --datalake.iceberg.credential $USER_CLIENT_ID:$USER_CLIENT_SECRET"
echo $USER_CLIENT_ID:$USER_CLIENT_SECRET > client_credentials