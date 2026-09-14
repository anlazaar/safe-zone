def services = [
    'media-service',
    'user-service',
    'product-service',
    'api-gateway',
    'eureka',
    // 'audit-service'
]

pipeline {
    agent none

    parameters {
        choice(
            name: 'TEST_SCOPE',
            choices: ['all', 'backend', 'frontend', 'none'],
            description: 'Select which tests to run'
        )

        booleanParam(
            name: 'ROLLBACK_ON_FAILURE',
            defaultValue: true,
            description: 'Automatically rollback if deployment verification fails'
        )

        booleanParam(
            name: 'SEND_NOTIFICATIONS',
            defaultValue: true,
            description: 'Send email notifications for build results'
        )
    }

    environment {
        IMAGE_TAG = "1.0.${BUILD_NUMBER}"
        NOTIFICATION_EMAIL = "amine.yacoubi.med@gmail.com"

        JWT_SECRET = credentials('JWT_SECRET')
        GATEWAY_KEYSTORE_PASSWORD = credentials('GATEWAY_KEYSTORE_PASSWORD')
        MINIO_ROOT_USER = credentials('MINIO_ROOT_USER')
        MINIO_ROOT_PASSWORD = credentials('MINIO_ROOT_PASSWORD')
        MONGO_ROOT_USERNAME = credentials('MONGO_ROOT_USERNAME')
        MONGO_ROOT_PASSWORD = credentials('MONGO_ROOT_PASSWORD')
        ADMIN_NAME = credentials('ADMIN_NAME')
        ADMIN_EMAIL = credentials('ADMIN_EMAIL')
        ADMIN_PASSWORD = credentials('ADMIN_PASSWORD')
    }

    stages {

        stage('Backend CI') {
            agent {
                label 'backend'
            }

            stages {

                stage('Checkout') {
                    steps {
                        checkout scm
                    }
                }

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
                    when {
                        expression {
                            params.TEST_SCOPE == 'all' ||
                            params.TEST_SCOPE == 'backend'
                        }
                    }

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
                label 'frontend'
            }

            stages {

                stage('Checkout') {
                    steps {
                        checkout scm
                    }
                }
        
                stage('Build') {
                    steps {
                        dir('frontend') {
                            sh 'npm ci'
                            sh 'npm run build'
                        }
                    }
                }

                stage('Test') {
                    when {
                        expression {
                            params.TEST_SCOPE == 'all' ||
                            params.TEST_SCOPE == 'frontend'
                        }
                    }

                    steps {
                        dir('frontend') {
                            sh 'npm test -- --watch=false --browsers=ChromeHeadless'
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            agent {
                label 'deployment'
            }

            steps {
                checkout scm

                withCredentials([
                    file(
                        credentialsId: 'TLS_KEY',
                        variable: 'TLS_KEY'
                    ),
                    file(
                        credentialsId: 'TLS_CRT',
                        variable: 'TLS_CRT'
                    ),
                    file(
                        credentialsId: 'GATEWAY_KEYSTORE',
                        variable: 'KEYSTORE_FILE'
                    )
                ]) {
                    sh '''
                        rm -f \
                            frontend/certs/frontend.key \
                            frontend/certs/frontend.crt \
                            backend/api-gateway/src/main/resources/gateway-keystore.p12

                        mkdir -p frontend/certs

                        cp "$TLS_KEY" frontend/certs/frontend.key
                        cp "$TLS_CRT" frontend/certs/frontend.crt

                        cp "$KEYSTORE_FILE" \
                            backend/api-gateway/src/main/resources/gateway-keystore.p12

                        docker compose \
                            -f docker-compose.jenkins.yml \
                            up -d --build
                    '''
                }
            }
        }

        stage('Deployment Verification') {
            agent {
                label 'deployment'
            }
            
            steps {
                script {
                    retry(6) {
                        def status = sh(
                            script: '''
                                docker inspect \
                                    --format='{{.State.Health.Status}}' \
                                    "$(docker compose ps -q api-gateway)"
                            ''',
                            returnStdout: true
                        ).trim()

                        echo "API Gateway health status: ${status}"
                        if (status != 'healthy') {
                            sleep 5
                            error("API Gateway is not healthy")
                        }
                    }

                    echo "Deployment ${env.IMAGE_TAG} is healthy."
                }
            }

            post {
                failure {
                    script {
                        echo "Deployment verification failed."

                        if (!params.ROLLBACK_ON_FAILURE) {
                            echo "Rollback is disabled."
                            error(
                                "Deployment verification failed and rollback is disabled."
                            )
                        }

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

                            docker compose \
                                -f docker-compose.jenkins.yml \
                                up -d --no-build
                        """

                        echo "Rollback to ${previousVersion} completed."

                        error(
                            "Deployment failed. " +
                            "Application rolled back to ${previousVersion}."
                        )
                    }
                }
            }
        }
    }

    post {

        success {
            script {
                if (params.SEND_NOTIFICATIONS) {
                    catchError(
                        buildResult: 'SUCCESS',
                        stageResult: 'UNSTABLE'
                    ) {
                        mail(
                            to: env.NOTIFICATION_EMAIL,
                            subject: "Build #${env.BUILD_NUMBER} — ${env.JOB_NAME} — SUCCESS",
                            body: """
                                Build completed successfully.

                                Job: ${env.JOB_NAME}
                                Build: #${env.BUILD_NUMBER}
                                Version: ${env.IMAGE_TAG}
                                Test scope: ${params.TEST_SCOPE}
                                Status: SUCCESS

                                The application was built, tested, and deployed successfully, for logs see ${env.BUILD_URL}.
                            """.stripIndent()
                        )
                    }
                }
            }
        }

        failure {
            script {
                if (params.SEND_NOTIFICATIONS) {
                    catchError(
                        buildResult: 'FAILURE',
                        stageResult: 'UNSTABLE'
                    ) {
                        mail(
                            to: env.NOTIFICATION_EMAIL,
                            subject: "Build #${env.BUILD_NUMBER} — ${env.JOB_NAME} — FAILURE",
                            body: """
                                Build failed.

                                Job: ${env.JOB_NAME}
                                Build: #${env.BUILD_NUMBER}
                                Version: ${env.IMAGE_TAG}
                                Test scope: ${params.TEST_SCOPE}
                                Status: FAILURE

                                Check the Jenkins console output for details.
                            """.stripIndent()
                        )
                    }
                }
            }
        }
    }
}
