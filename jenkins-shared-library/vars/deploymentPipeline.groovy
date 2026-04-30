def call(configMap) {
    pipeline {
        agent {
            node {
                label "AGENT-1"
            }
        }
        environment {
            REGION = "us-east-1"
            BRANCH = "${configMap.BRANCH}"
            GIT_URL = "${configMap.GIT_URL}"
            COMPONENT = "${configMap.COMPONENT}"
            CLUSTER_NAME = "roboshop-cluster"
        }
        options {
            timeout(time:15, unit: 'MINUTES')
            disableConcurrentBuilds()
        }
        stages {
            stage('checkout scm') {
                steps {
                    git url: "${GIT_URL}", branch: "${BRANCH}"
                }
            }
            stage('deploy') {
                steps {
                    dir("${COMPONENT}") {
                        withAWS(region:"${REGION}",credentials:'aws-creds') {
                            sh """
                            aws eks update-kubeconfig --region ${REGION} --name ${CLUSTER_NAME}
                            kubectl apply -f manifestfile.yaml
                            """
                        }
                    }
                }
            }
            stage('health-check') {
                steps {
                    withAWS(region:"${REGION}",credentials:'aws-creds') {
                        sh """
                        kubectl rollout status deployment ${COMPONENT} -n roboshop
                        """
                    } 
                }
            }
        }
    }
}