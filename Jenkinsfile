pipeline {
    agent any

    environment {
        APP_NAME        = "notification-service"
        NAMESPACE       = "next-me"

        REGISTRY        = "ghcr.io"
        GH_OWNER        = "sparta-next-me"
        IMAGE_REPO      = "notification-service"
        FULL_IMAGE      = "${REGISTRY}/${GH_OWNER}/${IMAGE_REPO}:latest"

        TZ              = "Asia/Seoul"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // Jenkins Credentials에 'notification-env' 파일이 등록되어 있어야 함
                withCredentials([
                    file(credentialsId: 'promotion-env', variable: 'ENV_FILE')
                ]) {
                    sh '''
                      set -a
                      . "$ENV_FILE"
                      set +a
                      ./gradlew clean bootJar --no-daemon
                    '''
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'ghcr-credential',
                        usernameVariable: 'USER',
                        passwordVariable: 'TOKEN'
                    )
                ]) {
                    sh """
                      docker build -t ${FULL_IMAGE} .
                      echo "${TOKEN}" | docker login ${REGISTRY} -u "${USER}" --password-stdin
                      docker push ${FULL_IMAGE}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([
                    file(credentialsId: 'k3s-kubeconfig', variable: 'KUBECONFIG_FILE'),
                    file(credentialsId: 'promotion-env', variable: 'ENV_FILE')
                ]) {
                    sh '''
                      export KUBECONFIG=${KUBECONFIG_FILE}

                      # 1. K8s Secret 업데이트 (YAML의 envFrom 이름과 일치)
                      echo "Updating K8s Secret: promotion-env..."
                      kubectl delete secret promotion-env -n ${NAMESPACE} --ignore-not-found
                      kubectl create secret generic promotion-env --from-env-file=${ENV_FILE} -n ${NAMESPACE}

                      # 2. 매니페스트 적용
                      echo "Applying manifests from notification-service.yaml..."
                      kubectl apply -f notification-service.yaml -n ${NAMESPACE}

                      # 3. 배포 모니터링
                      echo "Monitoring rollout status for ${APP_NAME}..."
                      kubectl rollout status deployment/notification-service -n ${NAMESPACE}
                    '''
                }
            }
        }
    }

    post {
        always {
            sh "docker rmi ${FULL_IMAGE} || true"
        }
        success {
            echo "Successfully deployed ${APP_NAME}!"
        }
        failure {
            echo "Deployment failed. Check Loki and Pod health."
        }
    }
}