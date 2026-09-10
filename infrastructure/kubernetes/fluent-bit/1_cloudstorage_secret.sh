source aws_creds.sh

echo $AWS_ACCESS_KEY_ID > aws_access_key_id
echo $AWS_SECRET_ACCESS_KEY > aws_secret_access_key
echo $AWS_REGION > aws_region
kubectl create secret generic aws-credentials \
    --from-file=aws_access_key_id \
    --from-file=aws_secret_access_key \
    --from-file=aws_region \
    --namespace fluent-bit