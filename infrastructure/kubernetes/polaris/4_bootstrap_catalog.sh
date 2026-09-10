# Script to create catalog, catalog role , principal role and attach catalog role to principal role

source aws_creds.sh

REALM=POLARIS

# Admin access creating base resources
ADMIN_CLIENT_ID=root
ADMIN_CLIENT_SECRET=s3cr3t

# Aws bucket
BUCKET_NAME=cloudconfidence414-data-warehouse
AWS_POLARIS_ROLE=cloudconfidence-buckets-rw-role

# Roles
PRINCIPAL_ROLE=rf_dev_elt
CATALOG_ROLE_FULL=rt_dev_catalog_full

# Catalog name
CATALOG_NAME=dev_catalog

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

echo "Creating catalog '$CATALOG_NAME' in realm $REALM..."
PAYLOAD='{
    "catalog": {
    "name": "'$CATALOG_NAME'",
    "type": "INTERNAL",
    "readOnly": false,
    "properties": {
        "default-base-location": "s3://'$BUCKET_NAME'/"
    },
    "storageConfigInfo": {
        "storageType": "S3",
        "roleArn": "arn:aws:iam::'$AWS_ACCOUNT_ID':role/'$AWS_POLARIS_ROLE'",
        "region": "'$AWS_REGION'"
    }
    }
}'
# DONT SPECIFY tenantId if AZURE_TENANT_ID is already specified !!!! see config :
# https://polaris.apache.org/releases/1.5.0/configuration/configuration-reference/#polarisfeaturesallow_unrestricted_storage_config_role_changes

RESPONSE=$(curl --fail-with-body -s -S -X POST http://localhost:8181/api/management/v1/catalogs \
    -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -H "Polaris-Realm: $REALM" \
    -d "$PAYLOAD" 2>&1) && echo -n "" || {
    echo "❌ Failed to create catalog"
    echo "$RESPONSE" >&2
    return
}
echo "✅ Catalog created"


echo "Creating principal role $PRINCIPAL_ROLE..."
RESPONSE=$(curl --fail-with-body -s -S -X POST http://localhost:8181/api/management/v1/principal-roles \
    -H "Authorization: Bearer $TOKEN" \
    -H "Polaris-Realm: $REALM" \
    -H "Content-Type: application/json" \
    -d '{"principalRole": {"name": "'$PRINCIPAL_ROLE'", "properties": {}}}' 2>&1) && echo -n "" || {
    echo "❌ Failed to create principal role"
    echo "$RESPONSE" >&2
    return
}
echo "✅ Principal role created"

echo "Creating catalog role '$CATALOG_ROLE_FULL'..."
RESPONSE=$(curl --fail-with-body -s -S -X POST http://localhost:8181/api/management/v1/catalogs/$CATALOG_NAME/catalog-roles \
    -H "Authorization: Bearer $TOKEN" \
    -H "Polaris-Realm: $REALM" \
    -H "Content-Type: application/json" \
    -d '{"catalogRole": {"name": "'$CATALOG_ROLE_FULL'", "properties": {}}}' 2>&1) && echo -n "" || {
    echo "❌ Failed to create catalog role"
    echo "$RESPONSE" >&2
    return
}
echo "✅ Catalog role created"



echo "Assigning catalog role to principal role..."
RESPONSE=$(curl --fail-with-body -s -S -X PUT http://localhost:8181/api/management/v1/principal-roles/$PRINCIPAL_ROLE/catalog-roles/$CATALOG_NAME \
    -H "Authorization: Bearer $TOKEN" \
    -H "Polaris-Realm: $REALM" \
    -H "Content-Type: application/json" \
    -d '{"catalogRole": {"name": "'$CATALOG_ROLE_FULL'"}}' 2>&1) && echo -n "" || {
    echo "❌ Failed to assign catalog role"
    echo "$RESPONSE" >&2
    return
}
echo "✅ Catalog role assigned"

echo "Granting CATALOG_MANAGE_CONTENT privilege to the role ..."
RESPONSE=$(curl --fail-with-body -s -S -X PUT http://localhost:8181/api/management/v1/catalogs/$CATALOG_NAME/catalog-roles/$CATALOG_ROLE_FULL/grants \
    -H "Authorization: Bearer $TOKEN" \
    -H "Polaris-Realm: $REALM" \
    -H "Content-Type: application/json" \
    -d '{"type": "catalog", "privilege": "CATALOG_MANAGE_CONTENT"}' 2>&1) && echo -n "" || {
    echo "❌ Failed to grant privileges"
    echo "$RESPONSE" >&2
    return
}
echo "✅ Privileges granted"