CATALOG_NAME=dev_catalog
REALM=POLARIS


RESPONSE=$(curl --fail-with-body -s -S -X DELETE http://localhost:8181/api/management/v1/catalogs/$CATALOG_NAME \
    -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -H "Polaris-Realm: $REALM" \
    ) && echo -n "" || {
    echo "❌ Failed to create catalog"
    echo "$RESPONSE" >&2
    return
}
echo "✅ Catalog deleted"
