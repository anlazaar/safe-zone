def services = [
    'media-service',
    'user-service',
    'product-service',
    'api-gateway',
    'eureka',
    // 'audit-service'
]

pipeline {
    agent any

    environment {
        IMAGE_TAG = "1.0.${BUILD_NUMBER}"
        NOTIFICATION_EMAIL = "amine.yacoubi.med@gmail.com"

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

        stage('Backend CI') {
            agent {
                docker {
                    image 'backend-agent:1.0'
                }
            }

            stages {

                stage('Build') {
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

                stage('Test') {
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
            }
        }

        stage('Frontend CI') {
            agent {
                docker {
                    image 'frontend-agent:1.0'
                    args '--privileged'
                }
            }

            stages {

                stage('Build') {
                    steps {
                        dir('frontend') {
                            sh 'npm ci'
                            sh 'npm run build'
                        }
                    }
                }

                stage('Test') {
                    steps {
                        dir('frontend') {
                            sh 'npm test -- --watch=false --browsers=ChromeHeadless'
                        }
                    }
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

                        cp "$KEYSTORE_FILE" \
                        backend/api-gateway/src/main/resources/gateway-keystore.p12

                        docker compose -f docker-compose.jenkins.yml up -d --build
                    '''
                }
            }
        }

        stage('Deployment Verification') {
            steps {
                script {
                    try {
                        retry(6) {

                            def status = sh(
                                script: "docker inspect --format='{{.State.Health.Status}}' mr-jenk-pipeline-api-gateway-1",
                                returnStdout: true
                            ).trim()

                            if (status != 'healthy') {
                                sleep 5
                                error("API Gateway is not healthy")
                            }
                        }

                        echo "Deployment ${env.IMAGE_TAG} is healthy."

                    } catch (Exception e) {

                        echo "Deployment verification failed."
                        echo "Starting rollback..."

                        def previousBuild = currentBuild.previousSuccessfulBuild

                        if (previousBuild == null) {
                            error(
                                "No previous successful deployment exists. " +
                                "Rollback cannot be performed."
                            )
                        }

                        def previousVersion = "1.0.${previousBuild.number}"

                        sh """
                            export IMAGE_TAG=${previousVersion}

                            echo "Rolling back to version: \$IMAGE_TAG"

                            docker compose -f docker-compose.jenkins.yml up -d --no-build
                        """

                        echo "Rollback to ${previousVersion} completed."

                        error(
                            "Deployment ${env.IMAGE_TAG} failed. " +
                            "Application rolled back to ${previousVersion}."
                        )
                    }
                }
            }
        }
    }

    post {

        success {
            catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                mail(
                    to: env.NOTIFICATION_EMAIL,
                    subject: "Build #${env.BUILD_NUMBER} — ${env.JOB_NAME} — SUCCESS",
                    body: """
                            Build completed successfully.

                            Job: ${env.JOB_NAME}
                            Build: #${env.BUILD_NUMBER}
                            Version: ${env.IMAGE_TAG}
                            Status: SUCCESS

                            The application was built, tested, and deployed successfully.
                        """.stripIndent()
                )
            }
        }

        failure {
            catchError(buildResult: 'FAILURE', stageResult: 'UNSTABLE') {
                mail(
                    to: env.NOTIFICATION_EMAIL,
                    subject: "Build #${env.BUILD_NUMBER} — ${env.JOB_NAME} — FAILURE",
                    body: """
                            Build failed.

                            Job: ${env.JOB_NAME}
                            Build: #${env.BUILD_NUMBER}
                            Version: ${env.IMAGE_TAG}
                            Status: FAILURE
                        """.stripIndent()
                )
            }
        }
    }

}
