pipeline {
  agent { label 'deocker-maven' }
  tools {
    maven 'maven3'
  }
  environment{
    SONAR_HOST = 'sonarqube.example.com'
    AWS_ACCOUNT_ID = 'YOUR_AWS_ACCOUNT_ID'
    AWS_REGION = 'us-east-1'
    EKS_CLUSTER_NAME = 'your-eks-cluster-name'
    ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    IMAGE_REPO = "${ECR_REGISTRY}/kami-devsec-proj"
    TMPDIR = '/opt/trivy-tmp'
  }
  stages {
    stage('Trivy Fs Scan') {
      steps {
        sh 'trivy fs --exit-code 1 --severity HIGH,CRITICAL .'
      }
    }
    stage('build & Sonar'){
      steps{
        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
    sh 'mvn clean verify sonar:sonar \
  -Dsonar.projectKey=kami-decsec-proj \
  -Dsonar.host.url="http://${SONAR_HOST}:9000" \
  -Dsonar.token="${SONAR_TOKEN}" \
  -Dsonar.qualitygate.wait=true'
}
      }
    }
    stage('ECR Login'){
      steps{
        sh 'aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY '
      }
    }
    stage('Build Image'){
    steps{
        sh 'export DOCKER_BUILDKIT=0 && docker build --platform linux/amd64 -t "$IMAGE_REPO:$BUILD_NUMBER" .'
    }
}
    stage('Trivy Image Scan') {
      steps {
        sh '''
            TMPDIR=/opt/trivy-tmp trivy image --exit-code 1 --severity HIGH,CRITICAL "$IMAGE_REPO:$BUILD_NUMBER"
            rm -rf /opt/trivy-tmp/*
        '''
          }
     }
    stage('Push to ECR') {
      steps {
        sh 'docker push "$IMAGE_REPO:$BUILD_NUMBER"'
        }
     }
    stage('Update Deployment') {
     steps {
        sh 'sed -i "s|image:.*|image: $IMAGE_REPO:$BUILD_NUMBER|g" deploy-svc.yaml'
      }
    }
    stage('Deploy to Kubernetes') {
  steps {
    sh '''#!/bin/bash -l
aws eks update-kubeconfig \
  --region $AWS_REGION \
  --name $EKS_CLUSTER_NAME \
  --kubeconfig /home/jenkins/.kube/config

kubectl create ns kami-devsecops
kubectl apply -f deploy-svc.yaml

kubectl rollout status -n kami-devsecops deployment/kami-devsec-proj --timeout=60s || {
  kubectl rollout undo -n kami-devsecops deployment/kami-devsec-proj || true
  exit 1
}
'''
  }
}
    
  }
}