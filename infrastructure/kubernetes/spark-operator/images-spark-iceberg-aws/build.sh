IMAGE_NAME=spark-iceberg-aws
IMAGE_TAG=0.1.0

eval $(minikube docker-env)

docker build -t $IMAGE_NAME:$IMAGE_TAG .