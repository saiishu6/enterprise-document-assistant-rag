
    pipeline {
        agent {
            label 'java25'
        }


        environment {
            QDRANT_HOST = 'qdrant'
            QDRANT_PORT = '6334'
            DOCKER_IMAGE = 'knowledge-assistant-api'
            CONTAINER_NAME = 'rag-ci-test'
        }


        options {
            timestamps()
        }

        stages {
            stage('Checkout') {
                steps {
                    git branch: 'main',
                        url: 'https://github.com/saiishu6/enterprise-document-assistant-rag.git'
                }
            }

            stage('Verify Tools') {
                steps {
                    sh 'java -version'
                    sh 'mvn -version'
                    sh 'git --version'
                }
            }

            stage('Build and Test') {
                environment {
                    OPENAI_API_KEY = 'test-key-for-ci'
                }
                steps {
                   withEnv(['QDRANT_HOST=localhost']) {
                               sh 'mvn -B clean verify'
                   }
                }
            }


            stage('Docker Build') {
                steps {
                     sh '''
                         docker build \
                        -t knowledge-assistant-api:${BUILD_NUMBER} \
                        -t knowledge-assistant-api:latest \
                         .
                        '''
                }
            }


            stage('Deploy') {
                steps {
                    withCredentials([
                        string(
                            credentialsId: 'openai-api-key',
                            variable: 'OPENAI_API_KEY'
                        )
                    ]) {
                        sh '''
                            docker rm -f rag-ci-test 2>/dev/null || true

                            docker run -d \
                              --name rag-ci-test \
                              --network rag-network \
                              -p 9091:8080 \
                              -e OPENAI_API_KEY \
                              -e QDRANT_HOST=qdrant \
                              -e QDRANT_PORT=6334 \
                              -e QDRANT_REST_PORT=6333 \
                              knowledge-assistant-api:${BUILD_NUMBER}
                        '''
                    }
                }
            }


            stage('Verify Kubernetes Authentication') {
                steps {
                    withCredentials([
                        string(
                            credentialsId: 'minikube-deployer-token',
                            variable: 'K8S_TOKEN'
                        )
                    ]) {
                        sh '''
                            curl --fail --silent --show-error \
                              --output /dev/null \
                              --write-out 'Kubernetes API HTTP status: %{http_code}\n' \
                              --cacert /tmp/minikube-ca.crt \
                              -H "Authorization: Bearer $K8S_TOKEN" \
                              https://minikube:8443/apis/apps/v1/namespaces/rag-app/deployments/knowledge-assistant-api
                        '''
                    }
                }
            }


            stage('Import Image into Minikube') {
                steps {
                    sh '''
                        set -eu
                        docker save "knowledge-assistant-api:${BUILD_NUMBER}" | \
                          docker exec -i minikube ctr -n k8s.io images import -
                    '''
                }
            }

            stage('Deploy to Kubernetes') {
                steps {
                    withCredentials([
                        string(credentialsId: 'minikube-deployer-token', variable: 'K8S_TOKEN')
                    ]) {
                        sh '''
                            set -eu
                            set +x
                            printf '{"spec":{"template":{"spec":{"containers":[{"name":"knowledge-assistant-api","image":"knowledge-assistant-api:%s"}]}}}}' "$BUILD_NUMBER" > k8s-image-patch.json
                            curl --fail --silent --show-error \
                              --output /dev/null \
                              --cacert /tmp/minikube-ca.crt \
                              -H "Authorization: Bearer $K8S_TOKEN" \
                              -H 'Content-Type: application/strategic-merge-patch+json' \
                              --request PATCH \
                              --data-binary @k8s-image-patch.json \
                              https://minikube:8443/apis/apps/v1/namespaces/rag-app/deployments/knowledge-assistant-api
                            rm -f k8s-image-patch.json
                            echo 'Kubernetes Deployment image updated.'
                        '''
                    }
                }
            }

            stage('Verify Kubernetes Rollout') {
                steps {
                    withCredentials([
                        string(credentialsId: 'minikube-deployer-token', variable: 'K8S_TOKEN')
                    ]) {
                        script {
                            def complete = false
                            for (int attempt = 1; attempt <= 36; attempt++) {
                                sh '''
                                    set +x
                                    curl --fail --silent --show-error \
                                      --cacert /tmp/minikube-ca.crt \
                                      -H "Authorization: Bearer $K8S_TOKEN" \
                                      --output k8s-rollout-status.json \
                                      https://minikube:8443/apis/apps/v1/namespaces/rag-app/deployments/knowledge-assistant-api
                                '''
                                def deployment = readJSON file: 'k8s-rollout-status.json', returnPojo: true
                                def desired = deployment.spec.replicas ?: 1
                                def status = deployment.status ?: [:]
                                def generation = deployment.metadata.generation ?: 0
                                def observed = status.observedGeneration ?: 0
                                def updated = status.updatedReplicas ?: 0
                                def ready = status.readyReplicas ?: 0
                                def available = status.availableReplicas ?: 0
                                def image = deployment.spec.template.spec.containers.find { it.name == 'knowledge-assistant-api' }?.image
                                echo "Rollout ${attempt}/36: observed=${observed}/${generation}, updated=${updated}/${desired}, ready=${ready}/${desired}, available=${available}/${desired}"
                                if (image == "knowledge-assistant-api:${env.BUILD_NUMBER}" &&
                                    observed >= generation && updated >= desired &&
                                    ready >= desired && available >= desired) {
                                    complete = true
                                    break
                                }
                                if (status.conditions?.any { it.type == 'Progressing' && it.status == 'False' && it.reason == 'ProgressDeadlineExceeded' }) {
                                    error('Kubernetes Deployment exceeded its progress deadline.')
                                }
                                if (attempt < 36) {
                                    sleep time: 5, unit: 'SECONDS'
                                }
                            }
                            if (!complete) {
                                error('Kubernetes rolling deployment did not become ready within approximately 3 minutes.')
                            }
                            echo 'Kubernetes rolling deployment completed successfully.'
                        }
                    }
                }
            }


        }

        post {
            always {
                junit allowEmptyResults: true,
                      testResults: '**/target/surefire-reports/*.xml'

                archiveArtifacts allowEmptyArchive: true,
                    artifacts: '**/target/site/jacoco/**'
            }
        }
    }
