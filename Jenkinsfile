def services = [
    'media-service',
    'user-service',
    'product-service',
    'api-gateway',
    'eureka'
    'audit-service'
]

pipeline {
    agent any

    environment {
        JWT_SECRET= credentials('JWT_SECRET')
        GATEWAY_KEYSTORE_PASSWORD= credentials('GATEWAY_KEYSTORE_PASSWORD')
        MINIO_ROOT_USER= credentials('MINIO_ROOT_USER')
        MINIO_ROOT_PASSWORD= credentials('MINIO_ROOT_PASSWORD')
        MONGO_ROOT_USERNAME= credentials('MONGO_ROOT_USERNAME')
        MONGO_ROOT_PASSWORD= credentials('MONGO_ROOT_PASSWORD')
        ADMIN_NAME= credentials('ADMIN_NAME')
        ADMIN_EMAIL= credentials('ADMIN_EMAIL')
        ADMIN_PASSWORD= credentials('ADMIN_PASSWORD')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Build') {
            steps {
                script {
                    def builds = [:]

                    services.each { service ->
                        def currentService = service

                        builds[currentService] = {
                            dir("backend/${currentService}") {
                                sh './mvnw package -DskipTests'
                            }
                        }
                    }

                    parallel builds
                }
            }
        }

        stage('Backend Tests') {
            steps {
                script {
                    def tests = [:]

                    services.each { service ->
                        def currentService = service

                        tests[currentService] = {
                            dir("backend/${currentService}") {
                                sh './mvnw test'
                            }
                        }
                    }

                    parallel tests
                }
            }
        }
        
        stage('Deploy') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'GATEWAY_KEYSTORE',
                        variable: 'KEYSTORE_FILE'
                    )
                ]) {
                    sh '''
                        rm -f backend/api-gateway/src/main/resources/gateway-keystore.p12
                        cp "$KEYSTORE_FILE" backend/api-gateway/src/main/resources/gateway-keystore.p12

                        docker compose up -d --build
                    '''
                }
            }
        }

        stage('Deployment Verification') {
            steps {
                script {
                    retry(6) {
                        def status = sh(
                            script: "docker inspect --format='{{.State.Health.Status}}' mr-jenk-pipeline-api-gateway-1",
                            returnStdout: true
                        ).trim()

                        echo "API Gateway health: ${status}"

                        if (status != 'healthy') {
                            sleep 5
                            error("Retrying now")
                        }
                    }
                }
            }
        }

    }
}
